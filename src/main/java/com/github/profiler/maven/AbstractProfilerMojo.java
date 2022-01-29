package com.github.profiler.maven;

import com.github.profiler.ProfilerEntryPoint;
import com.github.profiler.commons.functional.PredicateFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.pitest.classpath.ClassPath;
import org.pitest.functional.predicate.Predicate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractProfilerMojo extends AbstractMojo {
    private Predicate<String> appClassFilter;

    private Predicate<String> testClassFilter;

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "plugin.artifactMap", readonly = true, required = true)
    protected Map<String, Artifact> pluginArtifactMap;

    // -----------------------
    // ---- plugin params ----
    // -----------------------

    @Parameter(property = "sampleSize", defaultValue = "" + Integer.MAX_VALUE)
    protected int sampleSize;

    @Parameter(property = "targetClasses")
    protected Set<String> targetClasses;

    @Parameter(property = "excludedClasses")
    protected Set<String> excludedClasses;

    @Parameter(property = "targetTests")
    protected Set<String> targetTests;

    @Parameter(property = "excludedTests")
    protected Set<String> excludedTests;

    @Parameter(property = "childProcessArguments")
    protected List<String> childProcessArguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkAndSanitizeParameters();

        final ClassPath classPath = createClassPath();

        final File appBuildDirectory = new File(this.project.getBuild().getOutputDirectory());
        final File testBuildDirectory = new File(this.project.getBuild().getTestOutputDirectory());

        (new ProfilerEntryPoint(classPath,
                appBuildDirectory,
                testBuildDirectory,
                this.appClassFilter,
                this.testClassFilter,
                this.sampleSize,
                this.childProcessArguments)).start();
    }

    private void checkAndSanitizeParameters() throws MojoFailureException {
        if (this.sampleSize < 0) {
            throw new MojoFailureException("Bad sample size");
        }

        final String groupId = this.project.getGroupId();

        if (this.excludedClasses == null) {
            this.excludedClasses = Collections.emptySet();
        }
        final Predicate<String> excludedClassFilter = PredicateFactory.orGlobs(this.excludedClasses);

        if (this.targetClasses == null) {
            this.targetClasses = new HashSet<>();
        }
        if (this.targetClasses.isEmpty()) {
            this.targetClasses.add(groupId + ".*");
        }
        this.appClassFilter = PredicateFactory.orGlobs(this.targetClasses);
        this.appClassFilter = PredicateFactory.and(this.appClassFilter, PredicateFactory.not(excludedClassFilter));

        if (this.excludedTests == null) {
            this.excludedTests = Collections.emptySet();
        }
        final Predicate<String> excludedTestFilter = PredicateFactory.orGlobs(this.excludedTests);

        if (this.targetTests == null) {
            this.targetTests = new HashSet<>();
        }
        if (this.targetTests.isEmpty()) {
            this.targetTests.add(String.format("%s*Test", groupId));
            this.targetTests.add(String.format("%s*Tests", groupId));
        }
        this.testClassFilter = PredicateFactory.orGlobs(this.targetTests);
        this.testClassFilter = PredicateFactory.and(this.testClassFilter, PredicateFactory.not(excludedTestFilter));

        if (this.childProcessArguments == null) {
            this.childProcessArguments = Collections.singletonList("-Xmx64g");
        }
    }

    private List<File> getProjectClassPath() {
        final List<File> classPath = new ArrayList<>();
        try {
            for (final Object cpElement : this.project.getTestClasspathElements()) {
                classPath.add(new File((String) cpElement));
            }
        } catch (DependencyResolutionRequiredException e) {
            getLog().warn(e);
        }
        return classPath;
    }

    private List<File> getPluginClassPath() {
        final List<File> classPath = new ArrayList<>();
        for (Artifact dependency : this.pluginArtifactMap.values()) {
            if (isRelevantDep(dependency)) {
                classPath.add(dependency.getFile());
            }
        }
        return classPath;
    }

    private static boolean isRelevantDep(final Artifact dependency) {
        return dependency.getGroupId().equals("com.github")
                && dependency.getArtifactId().equals("profiler-maven-plugin");
    }

    private ClassPath createClassPath() {
        final List<File> classPathElements = new ArrayList<>();
        classPathElements.addAll(getProjectClassPath());
        classPathElements.addAll(getPluginClassPath());
        return new ClassPath(classPathElements);
    }
}