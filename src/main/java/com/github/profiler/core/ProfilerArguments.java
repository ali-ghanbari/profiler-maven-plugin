package com.github.profiler.core;

import org.apache.commons.lang3.Validate;
import org.pitest.functional.predicate.Predicate;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

class ProfilerArguments implements Serializable {
    private static final long serialVersionUID = 1L;

    final Predicate<String> appClassFilter;

    final Collection<String> testClassNames;

    public ProfilerArguments(final Predicate<String> appClassFilter,
                             final Collection<String> testClassNames) {
        Validate.isInstanceOf(Serializable.class, appClassFilter);
        Validate.isInstanceOf(Serializable.class, testClassNames);
        this.appClassFilter = appClassFilter;
        this.testClassNames = new HashSet<>(testClassNames);
    }
}