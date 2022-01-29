package com.github.profiler.core;

import com.github.profiler.commons.ControlId;
import com.github.profiler.commons.process.ChildProcessReporter;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

class ProfilerReporter extends ChildProcessReporter {
    public ProfilerReporter(final OutputStream os) {
        super(os);
    }

    public synchronized void reportMethodsTiming(final Map<Integer, Double> methodsTiming) {
        this.dos.writeByte(ControlId.PROFILER_REPORT_METHODS_TIMING);
        this.dos.write((Serializable) methodsTiming);
        this.dos.flush();
    }
}