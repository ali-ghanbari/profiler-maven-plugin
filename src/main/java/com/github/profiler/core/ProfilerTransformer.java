package com.github.profiler.core;

import com.github.profiler.commons.asm.ComputeClassWriter;
import com.github.profiler.commons.relational.MethodsDom;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.functional.predicate.Predicate;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import static org.pitest.bytecode.FrameOptions.pickFlags;

class ProfilerTransformer implements ClassFileTransformer {
    private static final Map<String, String> CACHE = new HashMap<>();

    private final ClassByteArraySource  byteArraySource;

    private final Predicate<String> appClassFilter;

    private final MethodsDom methodsDom;

    public ProfilerTransformer(final ClassByteArraySource byteArraySource,
                               final Predicate<String> appClassFilter,
                               final MethodsDom methodsDom) {
        this.byteArraySource = byteArraySource;
        this.appClassFilter = appClassFilter;
        this.methodsDom = methodsDom;
    }

    private boolean isAppClass(String className) {
        className = className.replace('/', '.');
        return this.appClassFilter.apply(className);
    }

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String className,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] classfileBuffer) {
        if (!isAppClass(className)) {
            return null; // no transformation
        }
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter classWriter = new ComputeClassWriter(this.byteArraySource, CACHE, pickFlags(classfileBuffer));
        final ClassVisitor classVisitor = new ProfilerTransformerClassVisitor(classWriter, this.methodsDom);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }
}