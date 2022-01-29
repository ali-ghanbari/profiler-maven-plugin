package com.github.profiler.commons.testing.junit.runner;

import com.github.profiler.commons.misc.Ansi;
import com.github.profiler.commons.misc.NameUtils;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;

class DefaultResultCollector implements ResultCollector {
    @Override
    public void notifyEnd(Description description, Throwable t) {
        System.out.flush();
        System.err.println();
        t.printStackTrace();
        System.err.println();
        System.err.flush();
    }

    @Override
    public void notifyEnd(Description description) {
        // nothing
    }

    @Override
    public void notifyStart(Description description) {
        final String testName = NameUtils.sanitizeExtendedTestName(description.getName());
        final String logMsg = Ansi.constructLogMessage("RUNNING",
                Ansi.ColorCode.MAGENTA, testName + "... ");
        System.out.println(logMsg);
    }

    @Override
    public void notifySkipped(Description description) {
        final String testName = NameUtils.sanitizeExtendedTestName(description.getName());
        System.out.println(Ansi.constructWarningMessage("SKIPPED", testName));
    }

    @Override
    public boolean shouldExit() {
        return false;
    }
}