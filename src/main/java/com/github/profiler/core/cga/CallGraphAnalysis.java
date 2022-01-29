package com.github.profiler.core.cga;

import com.github.profiler.commons.misc.Ansi;
import com.github.profiler.commons.relational.MethodsDom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classpath.ClassPath;
import soot.EntryPoints;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class CallGraphAnalysis {
    private MethodsDom methodsDom;

    private CallGraph cg;

    public void runSoot(final ClassPath classPath,
                        final File appBuildDirectory,
                        final File testBuildDirectory,
                        final ClassByteArraySource byteArraySource,
                        final File jreHome) throws Exception  {
        System.out.println(Ansi.constructInfoMessage("Info", "Finding entry points..."));
        final JUnitEntryPointFinder entryPointFinder =
                new JUnitEntryPointFinder(appBuildDirectory, testBuildDirectory, byteArraySource);
        entryPointFinder.discover();

        System.out.println(Ansi.constructInfoMessage("Info", "Running Soot..."));

        final CGASceneTransformer transformer = new CGASceneTransformer();
        PackManager.v()
                .getPack("wjtp")
                .add(new Transform("wjtp.mst", transformer));

        final String[] applicationClasses = entryPointFinder.getApplicationClasses();

        final File targetDirectory = new File(appBuildDirectory, "..");
        final String[] args = constructSootArgs(targetDirectory,
                jreHome, classPath, applicationClasses);
        Options.v().parse(args);

        for (final String applicationClass : applicationClasses) {
            try {
                SootClass clazz = Scene.v().loadClassAndSupport(applicationClass);
                clazz.setApplicationClass();
            } catch (final Throwable t) {
                System.out.println(Ansi.constructWarningMessage("Warning", t.getMessage()));
            }
        }
        Scene.v().loadNecessaryClasses();

        final List<SootMethod> entryPoints = new ArrayList<>();
        for (final String methodFullName : entryPointFinder.getEntryPoints()) {
            entryPoints.add(Scene.v().getMethod(methodFullName));
        }
        entryPoints.addAll(EntryPoints.v().all());
        Scene.v().setEntryPoints(entryPoints);

        PackManager.v().runPacks();

        this.methodsDom = transformer.getMethodsDom();
        this.cg = transformer.getCallGraph();
    }

    private static String[] constructSootArgs(final File targetDirectory,
                                              final File jreHome,
                                              final ClassPath classPath,
                                              final String[] applicationClasses) {
        final File jimpleTemp = new File(targetDirectory, "jimple-temp");
        jimpleTemp.deleteOnExit();
        String[] args = new String[]{
                "--w",
                "--f", "J",
                "--d", jimpleTemp.getAbsolutePath(),
                "--cp", constructSootClassPath(jreHome, classPath),
                "--p", "cg.spark", "enabled:false",
                "--p", "cg.cha", "enabled:true",
                "--src-prec", "class"
        };
        return ArrayUtils.addAll(args, applicationClasses);
    }


    private static String constructSootClassPath(final File compatibleJREHome,
                                                 final ClassPath classPath) {
        final File javaHome = new File(compatibleJREHome, "lib");
        final File[] jdkJARs = javaHome.listFiles(jarFileFilter());
        return String.format("%s%s%s",
                StringUtils.join(jdkJARs, File.pathSeparator),
                File.pathSeparator,
                classPath.getLocalClassPath());
    }

    private static FileFilter jarFileFilter() {
        return file -> file.isFile() && file.getName().endsWith(".jar");
    }

    public MethodsDom getMethodsDom() {
        return this.methodsDom;
    }

    public CallGraph getCallGraph() {
        return this.cg;
    }
}