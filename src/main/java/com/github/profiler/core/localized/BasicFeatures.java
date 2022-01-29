package com.github.profiler.core.localized;

public interface BasicFeatures {
    int getCyclomaticComplexity();

    int getNumInstructions();

    int getNumTypeInsn();

    int getNumParams();

    int getNumFieldAccesses();

    int getNumInvocations();

    int getNumHeapAllocations();

    int getNumLocals();
}
