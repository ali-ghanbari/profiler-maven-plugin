package com.github.profiler.core;

import com.github.profiler.commons.relational.MethodsDom;

import java.util.Map;

public interface ProfilerResults {
    MethodsDom getMethodsDom();

    Map<Integer, Double> getMethodTimingMap();
}