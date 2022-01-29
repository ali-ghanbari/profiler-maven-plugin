package com.github.profiler.core;

import com.github.profiler.commons.misc.Ansi;
import com.github.profiler.commons.process.ResourceUtils;
import com.github.profiler.commons.relational.MethodsDom;
import com.github.profiler.commons.testing.junit.runner.JUnitRunner;
import org.pitest.boot.HotSwapAgent;
import org.pitest.classinfo.CachingByteArraySource;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.functional.predicate.Predicate;
import org.pitest.process.ProcessArgs;
import org.pitest.util.ExitCode;
import org.pitest.util.IsolationUtils;
import org.pitest.util.SafeDataInputStream;

import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import static com.github.profiler.commons.Constants.BYTECODE_CLASS_CACHE_SIZE;

public final class Profiler {
    public static void main(String[] args) {
        System.out.println(Ansi.constructInfoMessage("Info", "Profiler started"));
        final int port = Integer.parseInt(args[0]);
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);

            final SafeDataInputStream dis = new SafeDataInputStream(socket.getInputStream());

            final ProfilerArguments arguments = dis.read(ProfilerArguments.class);

            final ClassLoader contextClassLoader = IsolationUtils.getContextClassLoader();
            ClassByteArraySource byteArraySource = new ClassloaderByteArraySource(contextClassLoader);
            byteArraySource = new CachingByteArraySource(byteArraySource, BYTECODE_CLASS_CACHE_SIZE);

            final Collection<String> testClassNames = arguments.testClassNames;

            final MethodsDom methodsDom = new MethodsDom();
            final ProfilerTransformer profilerTransformer = new ProfilerTransformer(byteArraySource,
                    arguments.appClassFilter,
                    methodsDom);
            HotSwapAgent.addTransformer(profilerTransformer);

            final long start = System.currentTimeMillis();
            (new JUnitRunner(testClassNames)).run();
            System.out.println(Ansi.constructInfoMessage("Info", "Profiling took " + (System.currentTimeMillis() - start) + " ms"));

            // finalizing & reporting the results
            final ProfilerReporter reporter = new ProfilerReporter(socket.getOutputStream());
            methodsDom.save(".", true);
            reporter.reportMethodsTiming(TimeRecorder.getMethodsTiming());
            System.out.println("Profiling is DONE!");
            reporter.done(ExitCode.OK);
        } catch (final Throwable throwable) {
            throwable.printStackTrace(System.out);
            System.out.println(Ansi.constructWarningMessage("WARNING", "Error during profiling!"));
        } finally {
            ResourceUtils.safelyCloseSocket(socket);
        }
    }


    public static ProfilerResults runProfiler(final ProcessArgs processArgs,
                                              final Predicate<String> appClassFilter,
                                              final Collection<String> testClassNames) throws Exception {
        final ProfilerArguments arguments = new ProfilerArguments(appClassFilter, testClassNames);
        final ProfilerProcess process = new ProfilerProcess(processArgs, arguments);
        process.start();
        process.waitToDie();
        return new ProfilerResults() {
            private final MethodsDom methodsDom = new MethodsDom(".");

            @Override
            public MethodsDom getMethodsDom() {
                return this.methodsDom;
            }

            @Override
            public Map<Integer, Double> getMethodTimingMap() {
                return process.getMethodsTiming();
            }
        };
    }
}