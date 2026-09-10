package dev.ftathiago.ciir.application.model;

import java.util.List;
import java.util.Objects;

/**
 * Operational outcome of an analysis run — not CIIR. Written verbatim (minus {@link #documents()}'s
 * {@code filesAnalyzed}, which only surfaces in {@code manifest.json}) as {@code
 * analysis-report.json}.
 */
public record AnalysisReport(
    boolean success,
    ProjectCounts projects,
    DocumentCounts documents,
    EntityCounts entities,
    RelationCounts relations,
    List<AnalysisError> errors) {

  public AnalysisReport {
    Objects.requireNonNull(projects, "projects");
    Objects.requireNonNull(documents, "documents");
    Objects.requireNonNull(entities, "entities");
    Objects.requireNonNull(relations, "relations");
    errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public record ProjectCounts(int discovered, int analyzed, int failed) {}

  public record DocumentCounts(int analyzed, int ignored, int filesAnalyzed) {}

  public record EntityCounts(int types, int methods) {}

  public record RelationCounts(int resolved, int unresolved) {}
}
