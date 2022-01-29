package com.github.profiler.core.localized;

import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import java.util.HashSet;
import java.util.Set;

class Node<V extends Value> extends Frame<V> {
    final Set<Node<V>> successors = new HashSet<>();

    public Node(final int nLocals, final int nStack) {
        super(nLocals, nStack);
    }

    public Node(final Frame<? extends V> src) {
        super(src);
    }
}