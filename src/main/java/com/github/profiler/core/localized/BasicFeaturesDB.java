package com.github.profiler.core.localized;

import com.github.profiler.commons.misc.Ansi;
import com.github.profiler.commons.relational.MethodsDom;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicFeaturesDB {
    private final Collection<File> classFiles;

    private final Map<Integer, BasicFeatures> db;

    public BasicFeaturesDB(final File appBuildDirectory, final MethodsDom methodsDom) throws IOException, AnalyzerException {
        this.classFiles = new HashSet<>();
        this.db = new HashMap<>();
        this.classFiles.addAll(FileUtils.listFiles(appBuildDirectory, new String[] {"class"}, true));
        for (Pair<String, MethodNode> pair : toMethodNodes()) {
            final String methodFullName = constructMethodFullName(pair.getLeft(),
                    pair.getRight().name,
                    pair.getRight().desc);
            final int index = methodsDom.indexOf(methodFullName);
            if (index < 0) {
                System.out.println(Ansi.constructWarningMessage("Warning", String.format("Method %s not found.", methodFullName)));
                continue;
            }
            this.db.put(index, new FeaturesExtractor(pair.getLeft(), pair.getRight()));
        }
    }

    private Collection<Pair<String /*owner*/, MethodNode>> toMethodNodes() throws IOException {
        final Collection<Pair<String /*owner*/, MethodNode>> results = new ArrayList<>();
        for (final File classFile : this.classFiles) {
            try (final InputStream is = new FileInputStream(classFile)) {
                final ClassNode classNode = new ClassNode(Opcodes.ASM7);
                final ClassReader classReader = new ClassReader(is);
                classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
                final String owner = classNode.name;
                for (final MethodNode methodNode : classNode.methods) {
                    if (methodNode.name.matches("<init>|<clinit>")) {
                        continue;
                    }
                    if (!Modifier.isAbstract(methodNode.access) && !Modifier.isNative(methodNode.access)) {
                        results.add(ImmutablePair.of(owner, methodNode));
                    }
                }
            }
        }
        return results;
    }

    private static String constructMethodFullName(final String className,
                                                  final String methodName,
                                                  final String descriptor) {
        return String.format("%s.%s(%s)",
                className.replace('/', '.'),
                methodName,
                Arrays.stream(Type.getArgumentTypes(descriptor))
                        .map(Type::getClassName)
                        .collect(Collectors.joining(","))
        );
    }

    public Map<Integer, BasicFeatures> getDB() {
        return this.db;
    }
}
