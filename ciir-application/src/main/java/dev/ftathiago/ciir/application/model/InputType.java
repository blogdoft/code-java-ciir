package dev.ftathiago.ciir.application.model;

/**
 * The kind of thing a resolved {@link AnalysisInput#path()} points to. {@code AGGREGATOR} is a
 * Maven {@code pom.xml} declaring {@code <modules>} (the role a C# {@code .sln} plays); {@code
 * PROJECT} is a leaf module {@code pom.xml}.
 */
public enum InputType {
  PROJECT,
  AGGREGATOR,
  DIRECTORY
}
