package com.github.profiler.commons.process;

import org.pitest.process.WrappingProcess;
import org.pitest.util.CommunicationThread;
import org.pitest.util.ExitCode;

import java.io.IOException;

public abstract class ChildProcess {
    protected final WrappingProcess process;

    protected final CommunicationThread communicationThread;

    protected ChildProcess(final WrappingProcess process,
                           final CommunicationThread communicationThread) {
        this.process = process;
        this.communicationThread = communicationThread;
    }

    public void start() throws IOException, InterruptedException {
        this.communicationThread.start();
        this.process.start();
    }

    public ExitCode waitToDie() {
        try {
            return this.communicationThread.waitToFinish();
        } finally {
            this.process.destroy();
        }
    }
}
