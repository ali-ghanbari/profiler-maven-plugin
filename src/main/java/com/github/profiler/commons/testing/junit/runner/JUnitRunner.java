package com.github.profiler.commons.testing.junit.runner;

import com.github.profiler.commons.misc.Ansi;
import com.github.profiler.commons.testing.LexTestComparator;
import com.github.profiler.commons.testing.TestComparator;
import org.pitest.functional.predicate.Predicate;
import org.pitest.testapi.ResultCollector;
import org.pitest.testapi.TestUnit;

import java.util.Collection;
import java.util.List;

import static com.github.profiler.commons.testing.junit.JUnitUtils.discoverTestUnits;

public class JUnitRunner {
    private List<TestUnit> testUnits;

    private final ResultCollector resultCollector;

    public JUnitRunner(final Collection<String> classNames) {
        this(classNames, new LexTestComparator());
    }

    public JUnitRunner(final Collection<String> classNames,
                       final TestComparator testComparator) {
        final List<TestUnit> testUnits = discoverTestUnits(classNames);
        testUnits.sort(testComparator);
        this.testUnits = testUnits;
        this.resultCollector = new DefaultResultCollector();
    }

    public JUnitRunner(final Collection<String> classNames,
                       final Predicate<String> failingTestFilter) {
        this(classNames, failingTestFilter, new LexTestComparator());
    }

    public JUnitRunner(final Collection<String> classNames,
                       final Predicate<String> failingTestFilter,
                       final TestComparator testComparator) {
        final List<TestUnit> testUnits = discoverTestUnits(classNames);
        testUnits.sort(testComparator);
        this.testUnits = testUnits;
        this.resultCollector = new EarlyExitResultCollector(new DefaultResultCollector(), failingTestFilter);
    }

    public List<TestUnit> getTestUnits() {
        return this.testUnits;
    }

    public void setTestUnits(List<TestUnit> testUnits) {
        this.testUnits = testUnits;
    }

    public boolean run() {
        return run(TestUnitFilter.all());
    }

    public boolean run(final Predicate<TestUnit> shouldRun) {
        for (final TestUnit testUnit : this.testUnits) {
            if (!shouldRun.apply(testUnit)) {
                continue;
            }
            testUnit.execute(this.resultCollector);
            if (this.resultCollector.shouldExit()) {
                final String logMsg = Ansi.constructWarningMessage("WARNING",
                        "Running test cases is terminated.");
                System.out.println(logMsg);
                return false;
            }
        }
        return true;
    }
}