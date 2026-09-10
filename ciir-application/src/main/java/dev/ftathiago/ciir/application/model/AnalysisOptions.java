package dev.ftathiago.ciir.application.model;

import java.nio.file.Path;
import java.util.Objects;

public record AnalysisOptions(
    Path outputPath,
    boolean verbose,
    boolean noProgress,
    boolean includeSource,
    boolean failOnError) {

  public AnalysisOptions {
    Objects.requireNonNull(outputPath, "outputPath");
  }
}
