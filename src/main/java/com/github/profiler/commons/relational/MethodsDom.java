package com.github.profiler.commons.relational;

public class MethodsDom extends StringDomain {

    public MethodsDom() {
        super("M");
    }

    public MethodsDom(final String pathName) {
        this();
        load(pathName);
    }
}