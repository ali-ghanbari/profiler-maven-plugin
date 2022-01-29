package com.github.profiler.core;

import com.github.profiler.commons.collections.LongStack;

import java.util.HashMap;
import java.util.Map;

import static com.github.profiler.commons.Constants.UNIT_SIZE;
import static com.github.profiler.commons.Constants.MIN_INVOCATION_COUNT;

public final class TimeRecorder {
    private static long mainThreadId;

    private static final LongStack[][] TIMESTAMPS;

    private static final long[][] EXEC_TIME_SUM;

    private static final int[][] INVOCATION_COUNT;

    static {
        TIMESTAMPS = new LongStack[UNIT_SIZE][];
        EXEC_TIME_SUM = new long[UNIT_SIZE][];
        INVOCATION_COUNT = new int[UNIT_SIZE][];
        mainThreadId = -1L;
    }

    private TimeRecorder() { }

    private static boolean threadSafe() {
        final long currentThreadId = Thread.currentThread().getId();
        if (mainThreadId == -1L) {
            mainThreadId = currentThreadId;
        }
        return mainThreadId == currentThreadId;
    }

    public static void registerEntryTimestamp(final int methodIndex) {
        if (threadSafe()) {
            final int unitIndex = methodIndex / UNIT_SIZE;
            final int index = methodIndex % UNIT_SIZE;
            final long timestamp = System.nanoTime();
            TIMESTAMPS[unitIndex][index].push(timestamp);
        }
    }

    public static void registerExitTimestamp(final int methodIndex) {
        if (threadSafe()) {
            final int unitIndex = methodIndex / UNIT_SIZE;
            final int index = methodIndex % UNIT_SIZE;
            final long entryTS = TIMESTAMPS[unitIndex][index].pop();
            final long timeElapsed = System.nanoTime() - entryTS;
            EXEC_TIME_SUM[unitIndex][index] += timeElapsed;
            INVOCATION_COUNT[unitIndex][index]++;
        }
    }

    static void allocateResources(final int methodIndex) {
        final int unitIndex = methodIndex / UNIT_SIZE;
        if (INVOCATION_COUNT[unitIndex] == null) {
            INVOCATION_COUNT[unitIndex] = new int[UNIT_SIZE];
        }
        if (EXEC_TIME_SUM[unitIndex] == null) {
            EXEC_TIME_SUM[unitIndex] = new long[UNIT_SIZE];
        }
        if (TIMESTAMPS[unitIndex] == null) {
            TIMESTAMPS[unitIndex] = new LongStack[UNIT_SIZE];
        }
        final int index = methodIndex % UNIT_SIZE;
        if (TIMESTAMPS[unitIndex][index] == null) {
            TIMESTAMPS[unitIndex][index] = new LongStack();
        }
    }

    static Map<Integer, Double> getMethodsTiming() {
        final Map<Integer, Double> methodsTiming = new HashMap<>();
        int methodId = 0;

        for (int unitIndex = 0; unitIndex < UNIT_SIZE; unitIndex++) {
            final int[] unit = INVOCATION_COUNT[unitIndex];
            if (unit == null) {
                methodId += UNIT_SIZE;
            } else {
                for (int index = 0; index < unit.length; index++) {
                    final int invocationsCount = unit[index];
                    if (invocationsCount >= MIN_INVOCATION_COUNT) {
                        final double nsTime = EXEC_TIME_SUM[unitIndex][index];
                        final double avg = (nsTime / 1_000D) / invocationsCount;
                        methodsTiming.put(methodId, avg);
                    }
                    methodId++;
                }
            }
        }
        return methodsTiming;
    }
}
