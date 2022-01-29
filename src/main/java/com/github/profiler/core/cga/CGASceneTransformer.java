package com.github.profiler.core.cga;

import com.github.profiler.commons.misc.Ansi;
import com.github.profiler.commons.relational.MethodsDom;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.Iterator;
import java.util.Map;

class CGASceneTransformer extends SceneTransformer {
    private MethodsDom methodsDom;

    private com.github.profiler.core.cga.CallGraph callGraph;

    @Override
    protected void internalTransform(String phaseName, Map options) {
        System.out.println(Ansi.constructInfoMessage("Info", "Constructing call-graph..."));
        final CallGraph callGraph = Scene.v().getCallGraph();

        this.methodsDom = new MethodsDom(".");
        for (final SootClass clazz : Scene.v().getClasses()) {
            for (final SootMethod method : clazz.getMethods()) {
                this.methodsDom.getOrAdd(getMethodFullName(method));
            }
        }

        this.callGraph = new com.github.profiler.core.cga.CallGraph();
        for (final SootClass clazz : Scene.v().getClasses()) {
            for (final SootMethod caller : clazz.getMethods()) {
                final int callerIndex = this.methodsDom.indexOf(getMethodFullName(caller));
                final Iterator<Edge> outEdges = callGraph.edgesOutOf(caller);
                while (outEdges.hasNext()) {
                    final Edge edge = outEdges.next();
                    final int calleeIndex = this.methodsDom.indexOf(getMethodFullName(edge.tgt()));
                    this.callGraph.addEdge(callerIndex, calleeIndex);
                }
            }
        }

    }

    public MethodsDom getMethodsDom() {
        return this.methodsDom;
    }

    public com.github.profiler.core.cga.CallGraph getCallGraph() {
        return this.callGraph;
    }

    public static String getMethodFullName(final SootMethod method) {
        SootClass cls = method.getDeclaringClass();
        String className = getClassName(cls);
        String subsig = method.getSubSignature();
        int sp = subsig.indexOf(' ');
        return className + "." + subsig.substring(sp + 1);
    }

    public static String getClassName(final SootClass cls) {
        return cls.getPackageName() + "." + cls.getJavaStyleName();
    }
}