package com.github.profiler.commons;

import static com.github.profiler.commons.misc.PropertyUtils.getIntProperty;

public final class Constants {
    public static final int UNIT_SIZE;

    public static final int BYTECODE_CLASS_CACHE_SIZE;

    public static final int MIN_INVOCATION_COUNT;

    static {
        UNIT_SIZE = getIntProperty("profiler.unit.capacity", 1024);
        BYTECODE_CLASS_CACHE_SIZE = getIntProperty("profiler.cache.size", 500);
        MIN_INVOCATION_COUNT = getIntProperty("profiler.min.invocations", 4);
    }

    private Constants() { }
}
