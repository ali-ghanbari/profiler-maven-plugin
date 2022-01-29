package com.github.profiler;

import com.github.profiler.commons.process.LoggerUtils;
import com.github.profiler.commons.relational.MethodsDom;
import com.github.profiler.core.Profiler;
import com.github.profiler.core.ProfilerResults;
import com.github.profiler.core.cga.CallGraph;
import com.github.profiler.core.cga.CallGraphAnalysis;
import com.github.profiler.core.localized.BasicFeatures;
import com.github.profiler.core.localized.BasicFeaturesDB;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.pitest.classinfo.CachingByteArraySource;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassInfo;
import org.pitest.classpath.ClassFilter;
import org.pitest.classpath.ClassPath;
import org.pitest.classpath.ClassPathByteArraySource;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.classpath.CodeSource;
import org.pitest.classpath.PathFilter;
import org.pitest.classpath.ProjectClassPaths;
import org.pitest.functional.Option;
import org.pitest.functional.predicate.Predicate;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.config.DefaultCodePathPredicate;
import org.pitest.mutationtest.config.DefaultDependencyPathPredicate;
import org.pitest.mutationtest.tooling.JarCreatingJarFinder;
import org.pitest.mutationtest.tooling.KnownLocationJavaAgentFinder;
import org.pitest.process.JavaAgent;
import org.pitest.process.JavaExecutableLocator;
import org.pitest.process.KnownLocationJavaExecutableLocator;
import org.pitest.process.LaunchOptions;
import org.pitest.process.ProcessArgs;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.profiler.commons.Constants.BYTECODE_CLASS_CACHE_SIZE;

public class ProfilerEntryPoint {
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator());

    private final File jreHome;

    private final ClassPath classPath;

    private final File appBuildDirectory;

    private final File testBuildDirectory;

    private final ClassByteArraySource byteArraySource;

    private final Predicate<String> appClassFilter;

    private final Predicate<String> testClassFilter;

    private final int sampleSize;

    private final List<String> childProcessArguments;

    public ProfilerEntryPoint(final ClassPath classPath,
                              final File appBuildDirectory,
                              final File testBuildDirectory,
                              final Predicate<String> appClassFilter,
                              final Predicate<String> testClassFilter,
                              final int sampleSize,
                              final List<String> childProcessArguments) throws MojoFailureException {
        final String jreHome = System.getProperty("java.home");
        if (jreHome == null) {
            throw new MojoFailureException("JAVA_HOME is not set");
        }
        this.jreHome = new File(jreHome);
        if (!this.jreHome.isDirectory()) {
            throw new MojoFailureException("Invalid JAVA_HOME");
        }
        this.classPath = classPath;
        this.appBuildDirectory = appBuildDirectory;
        this.testBuildDirectory = testBuildDirectory;
        this.byteArraySource = createClassByteArraySource(classPath);
        this.appClassFilter = appClassFilter;
        this.testClassFilter = testClassFilter;
        this.sampleSize = sampleSize;
        this.childProcessArguments = childProcessArguments;
    }

    public void start() throws MojoExecutionException {
        try {
            start0();
        } catch (final Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
    }

    public void start0() throws Exception {
        final Set<String> testClassNames = retrieveTestClassNames();
        final ProcessArgs defaultProcessArgs = getDefaultProcessArgs();

        final ProfilerResults profilerResults = Profiler.runProfiler(defaultProcessArgs, this.appClassFilter, testClassNames);

        final CallGraphAnalysis cga = new CallGraphAnalysis();

        cga.runSoot(this.classPath, this.appBuildDirectory, this.testBuildDirectory, this.byteArraySource, this.jreHome);

        // reporting output
        final CallGraph callGraph = cga.getCallGraph();
        final Map<Integer, Double> methodsTiming = profilerResults.getMethodTimingMap();
        final MethodsDom methodsDom = cga.getMethodsDom();
        final BasicFeaturesDB featuresDB = new BasicFeaturesDB(this.appBuildDirectory, methodsDom);
        try (final PrintWriter pw = new PrintWriter("MethodsFeatures.csv");
             final CSVPrinter printer = new CSVPrinter(pw, CSV_FORMAT)) {
            printer.printRecord("Method Name",
                    "#Instructions",
                    "#Type Instructions",
                    "#Parameters",
                    "#Field Accesses",
                    "#Method Invocations",
                    "#Heap Allocations",
                    "#Locals",
                    "Cyclomatic Complexity",
                    "Avg Time [us]");
            final Map<Integer, BasicFeatures> featuresMap = featuresDB.getDB();
            int sampleSize = this.sampleSize;
            for (final Map.Entry<Integer, Double> ent : methodsTiming.entrySet()) {
                if (sampleSize <= 0) {
                    break;
                }
                final int methodIndex = ent.getKey();
                final double avgTime = ent.getValue();
                if (featuresMap.containsKey(methodIndex)) {
                    int cyclomaticComplexity = 0;
                    int numInstructions = 0;
                    int numTypeInsn = 0;
                    int numParams = 0;
                    int numFieldAccesses = 0;
                    int numInvocations = 0;
                    int numHeapAllocations = 0;
                    int numLocals = 0;
                    final Set<Integer> reachableMethods = getReachableMethods(callGraph, methodIndex);
                    if (reachableMethods.size() > 20) { // a work-around for inaccuracies in the call graph
                        continue;
                    }
                    for (final int reachableMethodIndex : reachableMethods) {
                        if (featuresMap.containsKey(reachableMethodIndex)) {
                            final BasicFeatures basicFeatures = featuresMap.get(reachableMethodIndex);
                            cyclomaticComplexity += basicFeatures.getCyclomaticComplexity();
                            numInstructions += basicFeatures.getNumInstructions();
                            numTypeInsn += basicFeatures.getNumTypeInsn();
                            numParams += basicFeatures.getNumParams();
                            numFieldAccesses += basicFeatures.getNumFieldAccesses();
                            numInvocations += basicFeatures.getNumInvocations();
                            numHeapAllocations += basicFeatures.getNumHeapAllocations();
                            numLocals += basicFeatures.getNumLocals();
                        }
                    }
                    final String methodName = methodsDom.get(methodIndex);
                    printer.printRecord(methodName,
                            numInstructions,
                            numTypeInsn,
                            numParams,
                            numFieldAccesses,
                            numInvocations,
                            numHeapAllocations,
                            numLocals,
                            cyclomaticComplexity,
                            avgTime);
                    sampleSize--;
                }
            }
        }
    }

    private Set<Integer> getReachableMethods(final CallGraph cg, final int origin) {
        final Set<Integer> methods = new HashSet<>();
        cg.doDFS(origin, methods::add);
        return methods;
    }

    private Set<String> retrieveTestClassNames() {
        final ProjectClassPaths pcp = new ProjectClassPaths(this.classPath, defaultClassFilter(), defaultPathFilter());
        final CodeSource codeSource = new CodeSource(pcp);
        final Set<String> testClassNames = new HashSet<>();
        for (final ClassInfo classInfo : codeSource.getTests()) {
            testClassNames.add(classInfo.getName().asJavaName());
        }
        return testClassNames;
    }

    private static PathFilter defaultPathFilter() {
        return new PathFilter(new DefaultCodePathPredicate(),
                Prelude.not(new DefaultDependencyPathPredicate()));
    }

    private ClassFilter defaultClassFilter() {
        return new ClassFilter(this.testClassFilter, this.appClassFilter);
    }

    private ProcessArgs getDefaultProcessArgs() {
        final LaunchOptions defaultLaunchOptions = new LaunchOptions(getJavaAgent(),
                getDefaultJavaExecutableLocator(),
                this.childProcessArguments,
                Collections.<String, String>emptyMap());
        return ProcessArgs.withClassPath(this.classPath)
                .andLaunchOptions(defaultLaunchOptions)
                .andStderr(LoggerUtils.err())
                .andStdout(LoggerUtils.out());
    }

    private JavaExecutableLocator getDefaultJavaExecutableLocator() {
        final File javaFile = FileUtils.getFile(this.jreHome, "bin", "java");
        return new KnownLocationJavaExecutableLocator(javaFile.getAbsolutePath());
    }

    private JavaAgent getJavaAgent() {
        final String jarLocation = (new JarCreatingJarFinder(this.byteArraySource))
                .getJarLocation()
                .value();
        return new KnownLocationJavaAgentFinder(jarLocation);
    }

    private static ClassByteArraySource createClassByteArraySource(final ClassPath classPath) {
        final ClassPathByteArraySource cpbas = new ClassPathByteArraySource(classPath);
        final ClassByteArraySource cbas = fallbackToClassLoader(cpbas);
        return new CachingByteArraySource(cbas, BYTECODE_CLASS_CACHE_SIZE);
    }

    private static ClassByteArraySource fallbackToClassLoader(final ClassByteArraySource bas) {
        final ClassByteArraySource clSource = ClassloaderByteArraySource.fromContext();
        return clazz -> {
            final Option<byte[]> maybeBytes = bas.getBytes(clazz);
            if (maybeBytes.hasSome()) {
                return maybeBytes;
            }
            return clSource.getBytes(clazz);
        };
    }
}
