package com.github.profiler.commons.misc;

public final class NameUtils {
    private NameUtils() {

    }

    public static String sanitizeExtendedTestName(final String extendedTestName) {
        final int indexOfSpace = extendedTestName.indexOf(' ');
        return sanitizeTestName(extendedTestName.substring(1 + indexOfSpace));
    }

    public static String sanitizeTestName(String name) {
        //SETLab style: test.class.name:test_name
        name = name.replace(':', '.');
        //Defects4J style: test.class.name::test_name
        name = name.replace("..", ".");
        int indexOfLP = name.indexOf('(');
        if (indexOfLP >= 0) {
            final String testCaseName = name.substring(0, indexOfLP);
            name = name.substring(1 + indexOfLP, name.length() - 1) + "." + testCaseName;
        }
        return name;
    }
}