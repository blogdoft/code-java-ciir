package dev.ftathiago.ciir.application.model;

import java.nio.file.Path;
import java.util.Objects;

/** The resolved, absolute form of what the caller asked to analyze. */
public record AnalysisInput(InputType type, Path path) {

  public AnalysisInput {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(path, "path");
  }
}
