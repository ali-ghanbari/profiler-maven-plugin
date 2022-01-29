package com.github.profiler.core;

import com.github.profiler.commons.ControlId;
import org.pitest.functional.SideEffect1;
import org.pitest.util.CommunicationThread;
import org.pitest.util.ReceiveStrategy;
import org.pitest.util.SafeDataInputStream;
import org.pitest.util.SafeDataOutputStream;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

class ProfilerCommunicationThread extends CommunicationThread {
    private final DataReceiver receiver;

    public ProfilerCommunicationThread(final ServerSocket socket,
                                       final ProfilerArguments arguments) {
        this(socket, new DataSender(arguments), new DataReceiver());
    }

    private ProfilerCommunicationThread(final ServerSocket socket,
                                        final DataSender sender,
                                        final DataReceiver receiver) {
        super(socket, sender, receiver);
        this.receiver = receiver;
    }

    public Map<Integer, Double> getMethodsTiming() {
        return this.receiver.methodsTiming;
    }

    private static class DataSender implements SideEffect1<SafeDataOutputStream> {
        final ProfilerArguments arguments;

        DataSender(final ProfilerArguments arguments) {
            this.arguments = arguments;
        }

        @Override
        public void apply(final SafeDataOutputStream dos) {
            dos.write(this.arguments);
            dos.flush();
        }
    }

    private static class DataReceiver implements ReceiveStrategy {
        Map<Integer, Double> methodsTiming;

        @Override
        @SuppressWarnings({"unchecked"})
        public void apply(byte control, SafeDataInputStream is) {
            if (control == ControlId.PROFILER_REPORT_METHODS_TIMING) {
                this.methodsTiming = is.read(HashMap.class);
            }
        }
    }
}