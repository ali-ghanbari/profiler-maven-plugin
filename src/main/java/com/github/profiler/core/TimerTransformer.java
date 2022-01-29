package com.github.profiler.core;

import com.github.profiler.commons.asm.FinallyBlockAdviceAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class TimerTransformer extends FinallyBlockAdviceAdapter {
    private static final Type TIME_RECORDER = Type.getType(TimeRecorder.class);

    private final int methodIndex;

    public TimerTransformer(final MethodVisitor methodVisitor,
                            final int access,
                            final String name,
                            final String descriptor,
                            final int methodIndex) {
        super(ASM7, methodVisitor, access, name, descriptor);
        this.methodIndex = methodIndex;
    }

    @Override
    protected void insertPrelude() {
        push(this.methodIndex);
        invokeStatic(TIME_RECORDER, Method.getMethod("void registerEntryTimestamp(int)"));
    }

    @Override
    protected void insertSequel(boolean normalExit) {
        push(this.methodIndex);
        invokeStatic(TIME_RECORDER, Method.getMethod("void registerExitTimestamp(int)"));
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        TimeRecorder.allocateResources(this.methodIndex);
    }
}