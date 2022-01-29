package com.github.profiler.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
public class ProfilerMojo extends AbstractProfilerMojo {

}
