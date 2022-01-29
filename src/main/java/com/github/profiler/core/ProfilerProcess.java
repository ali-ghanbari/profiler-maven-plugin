package com.github.profiler.core;

import com.github.profiler.commons.process.ChildProcess;
import org.pitest.process.ProcessArgs;
import org.pitest.process.WrappingProcess;
import org.pitest.util.SocketFinder;

import java.net.ServerSocket;
import java.util.Map;

class ProfilerProcess extends ChildProcess {
    public ProfilerProcess(final ProcessArgs processArgs,
                           final ProfilerArguments arguments) {
        this((new SocketFinder()).getNextAvailableServerSocket(), processArgs, arguments);
    }

    private ProfilerProcess(final ServerSocket socket,
                            final ProcessArgs processArgs,
                            final ProfilerArguments arguments) {
        super(new WrappingProcess(socket.getLocalPort(), processArgs, Profiler.class),
                new ProfilerCommunicationThread(socket, arguments));
    }

    public Map<Integer, Double> getMethodsTiming() {
        return ((ProfilerCommunicationThread) this.communicationThread).getMethodsTiming();
    }
}