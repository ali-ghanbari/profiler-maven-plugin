package com.github.profiler.core.cga;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

public class CallGraph {
    private final Map<Integer, Set<Integer>> graph;

    public CallGraph() {
        this.graph = new HashMap<>();
    }

    void addEdge(final int caller, final int callee) {
        this.graph.computeIfAbsent(caller, __ -> new HashSet<>()).add(callee);
    }

    public Set<Integer> succs(final int node) {
        return this.graph.getOrDefault(node, Collections.emptySet());
    }

    public void doDFS(final int origin, final IntConsumer visitor) {
        doDFS(origin, visitor, new HashSet<>());
    }

    private void doDFS(final int node, final IntConsumer visitor, final Set<Integer> visited) {
        visitor.accept(node);
        visited.add(node);
        for (final int succ : succs(node)) {
            if (!visited.contains(succ)) {
                doDFS(succ, visitor, visited);
            }
        }
    }
}
