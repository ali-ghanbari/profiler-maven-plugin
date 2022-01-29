package com.github.profiler.core.cga;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ASM7;

class Crawler extends ClassVisitor {
    String className;

    String superName;

    final List<AnnotatedMethodSig> methods;

    public Crawler() {
        super(ASM7);
        this.methods = new ArrayList<>();
    }

    @Override
    public void visit(final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
        this.className = name.replace('/', '.');
        this.superName = superName == null ?
                null : superName.replace('/', '.');
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String descriptor,
                                     final String signature,
                                     final String[] exceptions) {
        final MethodVisitor methodVisitor =
                super.visitMethod(access, name, descriptor, signature, exceptions);
        if (Modifier.isPublic(access)) {
            // This is the format of method names expected by Soot:
            // <java.net.Inet6Address: void <init>(java.lang.String,byte[],int)>
            // <java.net.Inet4Address: java.lang.String numericToTextFormat(byte[])>
            // <java.net.Inet4Address: void <clinit>()>
            final String methodSig = String.format("<%s: %s %s(%s)>",
                    this.className,
                    Type.getReturnType(descriptor).getClassName(),
                    name,
                    Arrays.stream(Type.getArgumentTypes(descriptor))
                            .map(Type::getClassName)
                            .collect(Collectors.joining(","))
            );
            final AnnotatedMethodSig ams = new AnnotatedMethodSig(methodSig);
            this.methods.add(ams);
            return new CrawlerMethodVisitor(methodVisitor, ams);
        }
        return methodVisitor;
    }

    private static class CrawlerMethodVisitor extends MethodVisitor {
        private final AnnotatedMethodSig ams;

        public CrawlerMethodVisitor(final MethodVisitor methodVisitor,
                                    final AnnotatedMethodSig ams) {
            super(ASM7, methodVisitor);
            this.ams = ams;
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor,
                                                 boolean visible) {
            ams.addAnnotation(descriptor);
            return super.visitAnnotation(descriptor, visible);
        }
    }
}