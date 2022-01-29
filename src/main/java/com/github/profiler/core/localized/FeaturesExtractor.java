package com.github.profiler.core.localized;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.MULTIANEWARRAY;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NEWARRAY;

class FeaturesExtractor implements BasicFeatures {
    private int cyclomaticComplexity;

    private int numInstructions;

    private int numTypeInsn;

    private int numParams;

    private int numFieldAccesses;

    private int numInvocations;

    private int numHeapAllocations;

    private int numLocals;

    FeaturesExtractor(final String owner, final MethodNode mn) throws AnalyzerException {
        this.computeBasicFeatures(mn);
        this.computeCyclomaticComplexity(owner, mn);
    }

    @Override
    public int getCyclomaticComplexity() {
        return this.cyclomaticComplexity;
    }

    @Override
    public int getNumInstructions() {
        return this.numInstructions;
    }

    @Override
    public int getNumTypeInsn() {
        return this.numTypeInsn;
    }

    @Override
    public int getNumParams() {
        return this.numParams;
    }

    @Override
    public int getNumFieldAccesses() {
        return this.numFieldAccesses;
    }

    @Override
    public int getNumInvocations() {
        return this.numInvocations;
    }

    @Override
    public int getNumHeapAllocations() {
        return this.numHeapAllocations;
    }

    @Override
    public int getNumLocals() {
        return this.numLocals;
    }

    private void computeBasicFeatures(final MethodNode mn) {
        this.numInstructions = mn.instructions.size();
        this.numLocals = mn.localVariables.size();
        this.numParams = Type.getArgumentTypes(mn.desc).length;
        for (final AbstractInsnNode insn : mn.instructions) {
            if (insn instanceof FieldInsnNode) {
                this.numFieldAccesses++;
            } else if (insn instanceof TypeInsnNode) {
                switch (insn.getOpcode()) {
                    case NEW:
                    case NEWARRAY:
                    case ANEWARRAY:
                    case MULTIANEWARRAY:
                        this.numHeapAllocations++;
                        break;
                    default: this.numTypeInsn++;
                }

            } else if (insn instanceof MethodInsnNode) {
                this.numInvocations++;
            }
        }
    }

    private void computeCyclomaticComplexity(final String owner, final MethodNode mn) throws AnalyzerException {
        final Analyzer<BasicValue> a = new Analyzer<BasicValue>(new BasicInterpreter()) {
            protected Frame<BasicValue> newFrame(final int nLocals, final int nStack) {
                return new Node<>(nLocals, nStack);
            }

            @Override
            protected Frame<BasicValue> newFrame(final Frame<? extends BasicValue> src) {
                return new Node<>(src);
            }

            @Override
            protected void newControlFlowEdge(final int src, final int dst) {
                final Node<BasicValue> s = (Node<BasicValue>) getFrames()[src];
                s.successors.add((Node<BasicValue>) getFrames()[dst]);
            }
        };
        a.analyze(owner, mn);
        final Frame<BasicValue>[] frames = a.getFrames();
        int edges = 0;
        int nodes = 0;
        for (final Frame<BasicValue> frame : frames) {
            if (frame != null) {
                edges += ((Node<BasicValue>) frame).successors.size();
                nodes += 1;
            }
        }
        this.cyclomaticComplexity = edges - nodes + 2;
    }
}
