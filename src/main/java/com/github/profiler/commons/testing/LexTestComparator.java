package com.github.profiler.commons.testing;

import com.github.profiler.commons.misc.NameUtils;
import org.pitest.testapi.TestUnit;

public class LexTestComparator implements TestComparator {
    @Override
    public int compare(final TestUnit tu1, final TestUnit tu2) {
        final String n1 = NameUtils.sanitizeExtendedTestName(tu1.getDescription().getName());
        final String n2 = NameUtils.sanitizeExtendedTestName(tu2.getDescription().getName());
        return n1.compareTo(n2);
    }
}
