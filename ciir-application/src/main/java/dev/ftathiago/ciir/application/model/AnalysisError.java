package dev.ftathiago.ciir.application.model;

import java.util.Objects;

/**
 * @param category e.g. {@code "project_load"}.
 */
public record AnalysisError(String project, String message, String category) {

  public AnalysisError {
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(category, "category");
  }
}
