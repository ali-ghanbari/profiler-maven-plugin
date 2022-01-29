package com.github.profiler.commons.process;

import org.pitest.functional.SideEffect1;

public final class LoggerUtils {
    private static final Object LOCK = new Object();

    private LoggerUtils() {

    }

    public static SideEffect1<String> out() {
        return msg -> {
            synchronized (LOCK) {
                System.out.print(msg);
                System.out.flush();
            }
        };
    }

    public static SideEffect1<String> err() {
        return msg -> {
            synchronized (LOCK) {
                System.out.print(msg);
                System.out.flush();
            }
        };
    }
}