package com.github.profiler.core.cga;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.pitest.classinfo.ClassByteArraySource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

class JUnitEntryPointFinder {
    private static final String[] CLASS_FILE_EXT = {"class"};

    private final File appBuildDir;

    private final File testBuildDir;

    private final ClassByteArraySource byteArraySource;

    private final List<String> classes;

    private final List<String> testClasses;

    private final List<String> entryPoints;

    public JUnitEntryPointFinder(final File appBuildDir,
                                 final File testBuildDir,
                                 final ClassByteArraySource byteArraySource) {
        if (!testBuildDir.isDirectory()
                || !appBuildDir.isDirectory()
                || byteArraySource == null) {
            throw new IllegalArgumentException();
        }
        this.entryPoints = new ArrayList<>();
        this.classes = new ArrayList<>();
        this.testClasses = new ArrayList<>();
        this.appBuildDir = appBuildDir;
        this.testBuildDir = testBuildDir;
        this.byteArraySource = byteArraySource;
    }

    public void discover() throws Exception {
        final Collection<File> classFiles = listClassFiles(this.appBuildDir);
        analyzeClasses(classFiles, this.classes);

        final Collection<File> testClassFiles = listClassFiles(this.testBuildDir);
        analyzeClasses(testClassFiles, this.testClasses);
    }

    private static Collection<File> listClassFiles(final File directory) {
        return FileUtils.listFiles(directory, CLASS_FILE_EXT, true);
    }

    public String[] getApplicationClasses() {
        final List<String> appClasses = new ArrayList<>();
        appClasses.addAll(this.classes);
        appClasses.addAll(this.testClasses);
        return appClasses.toArray(new String[0]);
    }

    public String[] getEntryPoints() {
        return this.entryPoints.toArray(new String[0]);
    }

    private void analyzeClasses(final Collection<File> classFiles,
                                final Collection<String> classNames) throws Exception {
        final List<AnnotatedMethodSig> allMethods = new ArrayList<>();
        final Queue<String> hierarchy = new ArrayDeque<>();
        for (final File classFile : classFiles) {
            crawl(classFile, this.byteArraySource, hierarchy, allMethods);
            classNames.add(hierarchy.poll());
            addJUnit3XXEntryPoints(hierarchy, allMethods);
            addJUnit4YYEntryPoints(allMethods);
            allMethods.clear();
            hierarchy.clear();
        }
    }

    private void addJUnit3XXEntryPoints(final Queue<String> hierarchy,
                                        final List<AnnotatedMethodSig> allMethods) {
        boolean testClass = false;
        while (!hierarchy.isEmpty()) {
            if (hierarchy.poll().equals("junit.framework.TestCase")) {
                testClass = true;
                break;
            }
        }
        if (testClass) {
            for (final AnnotatedMethodSig annotatedSig : allMethods) {
                final String signature = annotatedSig.getMethodSig();
                if (isJUnit3XXEntryPoint(signature)) {
                    this.entryPoints.add(signature);
                }
            }
        }
    }

    private void addJUnit4YYEntryPoints(final List<AnnotatedMethodSig> allMethods) {
        for (final AnnotatedMethodSig annotatedSig : allMethods) {
            if (isJUnit4YYEntryPoint(annotatedSig)) {
                this.entryPoints.add(annotatedSig.getMethodSig());
            }
        }
    }

    private static boolean isJUnit4YYEntryPoint(final AnnotatedMethodSig a) {
        final List<Boolean> annotationsEffects = new ArrayList<>();
        for (final String descriptor : a.getAnnotations()) {
            if (descriptor.endsWith("/Ignore;")) {
                annotationsEffects.add(false);
            } else if (descriptor.endsWith("/Test;")
                    || descriptor.endsWith("/Before;")
                    || descriptor.endsWith("/After;")
                    || descriptor.endsWith("/BeforeClass;")
                    || descriptor.endsWith("/AfterClass;")
                    || descriptor.endsWith("/Parameterized$Parameters;")) {
                annotationsEffects.add(true);
            }
        }
        boolean isTestMethod = true;
        for (final Boolean e : annotationsEffects) {
            isTestMethod &= e;
        }
        return !annotationsEffects.isEmpty() && isTestMethod;
    }

    private void crawl(final File classFile,
                       final ClassByteArraySource byteArraySource,
                       final Queue<String> hierarchy,
                       final List<AnnotatedMethodSig> allMethods) throws IOException {
        Crawler crawler;
        byte[] classBytes = FileUtils.readFileToByteArray(classFile);

        while (classBytes != null) {
            crawler = new Crawler();
            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(crawler, ClassReader.EXPAND_FRAMES);
            hierarchy.offer(crawler.className);
            allMethods.addAll(crawler.methods);
            if (crawler.superName == null) { // reached the top of hierarchy
                break;
            }
            classBytes = byteArraySource.getBytes(crawler.superName)
                    .getOrElse(null);
        }
        if (classBytes == null) {
            throw new RuntimeException("We were unable to load a class. Is there any classpath problem?");
        }
    }

    private static boolean isJUnit3XXEntryPoint(final String methodSig) {
        return methodSig.endsWith("<clinit>()>")
                || methodSig.endsWith("void setUp()>")
                || methodSig.endsWith("void tearDown()>")
                || (methodSig.contains("void test") && methodSig.endsWith("()>"));
    }
}