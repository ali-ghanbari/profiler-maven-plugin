package com.github.profiler.core;

import com.github.profiler.commons.relational.MethodsDom;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ASM7;

class ProfilerTransformerClassVisitor extends ClassVisitor {
    private String classInternalName;

    private final MethodsDom methodsDom;

    private boolean isInterface;

    ProfilerTransformerClassVisitor(final ClassVisitor classVisitor, final MethodsDom methodsDom) {
        super(ASM7, classVisitor);
        this.methodsDom = methodsDom;
    }

    @Override
    public void visit(final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
        this.isInterface = Modifier.isInterface(access);
        this.classInternalName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private boolean isConstructor(final String name) {
        return name.matches("<init>|<clinit>");
    }

    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String descriptor,
                                     final String signature,
                                     final String[] exceptions) {
        MethodVisitor defaultMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (this.isInterface || Modifier.isAbstract(access) || Modifier.isNative(access)) {
            return defaultMethodVisitor;
        }
        if (isConstructor(name)) {
            return defaultMethodVisitor;
        }
        final String methodFullName = getMethodFullName(this.classInternalName, name, descriptor);
        final int methodIndex = this.methodsDom.getOrAdd(methodFullName);
        return new TimerTransformer(defaultMethodVisitor, access, name, descriptor, methodIndex);
    }

    public static String getMethodFullName(final String className,
                                           final String methodName,
                                           final String descriptor) {
        return String.format("%s.%s(%s)",
                className.replace('/', '.'),
                methodName,
                Arrays.stream(Type.getArgumentTypes(descriptor))
                        .map(Type::getClassName)
                        .collect(Collectors.joining(",")));
    }
}