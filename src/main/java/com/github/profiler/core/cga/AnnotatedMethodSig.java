package com.github.profiler.core.cga;

import java.util.HashSet;
import java.util.Set;

class AnnotatedMethodSig {
    private final String methodSig;

    private final Set<String> annotations;

    public AnnotatedMethodSig(String methodSig) {
        this.methodSig = methodSig;
        this.annotations = new HashSet<>();
    }

    public void addAnnotation(final String annotation) {
        this.annotations.add(annotation);
    }

    public String getMethodSig() {
        return this.methodSig;
    }

    public Set<String> getAnnotations() {
        return this.annotations;
    }
}