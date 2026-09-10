package dev.ftathiago.ciir.application.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Written verbatim as {@code manifest.json}. */
public record AnalysisManifest(
    String schemaVersion,
    String generatorVersion,
    InputType inputType,
    String inputPath,
    Instant generatedAt,
    List<ManifestProject> projects,
    List<ManifestFile> files,
    ManifestStatistics statistics) {

  public AnalysisManifest {
    Objects.requireNonNull(schemaVersion, "schemaVersion");
    Objects.requireNonNull(generatorVersion, "generatorVersion");
    Objects.requireNonNull(inputType, "inputType");
    Objects.requireNonNull(inputPath, "inputPath");
    Objects.requireNonNull(generatedAt, "generatedAt");
    Objects.requireNonNull(statistics, "statistics");
    projects = projects == null ? List.of() : List.copyOf(projects);
    files = files == null ? List.of() : List.copyOf(files);
  }

  public record ManifestProject(String name, String path) {}

  /**
   * @param records {@code null} for non-record-bearing files, e.g. {@code ciir.schema.json}.
   */
  public record ManifestFile(String path, Integer records, String sha256) {}

  public record ManifestStatistics(
      int projects,
      int filesAnalyzed,
      int types,
      int methods,
      int relations,
      int unresolvedRelations) {}
}
