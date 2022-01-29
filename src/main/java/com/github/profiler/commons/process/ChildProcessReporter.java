package com.github.profiler.commons.process;

import com.github.profiler.commons.ControlId;
import org.pitest.util.ExitCode;
import org.pitest.util.SafeDataOutputStream;

import java.io.OutputStream;

public abstract class ChildProcessReporter {
    protected final SafeDataOutputStream dos;

    protected ChildProcessReporter(final OutputStream os) {
        this.dos = new SafeDataOutputStream(os);
    }

    public synchronized void done(final ExitCode exitCode) {
        this.dos.writeByte(ControlId.DONE);
        this.dos.writeInt(exitCode.getCode());
        this.dos.flush();
    }
}
