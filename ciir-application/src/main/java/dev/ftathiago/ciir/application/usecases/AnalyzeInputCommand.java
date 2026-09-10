package dev.ftathiago.ciir.application.usecases;

import dev.ftathiago.ciir.application.model.AnalysisOptions;
import java.util.Objects;

public record AnalyzeInputCommand(String path, AnalysisOptions options) {

  public AnalyzeInputCommand {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(options, "options");
  }
}
