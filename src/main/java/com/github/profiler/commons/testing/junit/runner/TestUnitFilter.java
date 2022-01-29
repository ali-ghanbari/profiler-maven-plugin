package com.github.profiler.commons.testing.junit.runner;

import com.github.profiler.commons.misc.NameUtils;
import org.pitest.functional.predicate.Predicate;
import org.pitest.testapi.TestUnit;

import java.util.Collection;

public class TestUnitFilter {
    public static Predicate<TestUnit> all() {
        return testUnit -> Boolean.TRUE;
    }

    public static Predicate<TestUnit> some(final Collection<String> testUnitNames) {
        return testUnit -> {
            final String testName = NameUtils.sanitizeExtendedTestName(testUnit.getDescription().getName());
            return testUnitNames.contains(testName);
        };
    }
}