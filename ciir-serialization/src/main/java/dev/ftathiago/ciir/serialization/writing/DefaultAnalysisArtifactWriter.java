package dev.ftathiago.ciir.serialization.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ftathiago.ciir.application.model.AnalysisManifest;
import dev.ftathiago.ciir.application.model.AnalysisReport;
import dev.ftathiago.ciir.application.model.InputType;
import dev.ftathiago.ciir.application.ports.AnalysisArtifactWriter;
import dev.ftathiago.ciir.serialization.json.CiirObjectMapperFactory;
import dev.ftathiago.ciir.serialization.schema.CiirSchemaProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/** Writes {@code analysis-report.json}, {@code ciir.schema.json} and {@code manifest.json}. */
public final class DefaultAnalysisArtifactWriter implements AnalysisArtifactWriter {

  private final ObjectMapper objectMapper = CiirObjectMapperFactory.create();

  @Override
  public void writeReport(AnalysisReport report, Path outputDirectory) throws IOException {
    var payload =
        new ReportPayload(
            report.success(),
            new ProjectsPayload(
                report.projects().discovered(),
                report.projects().analyzed(),
                report.projects().failed()),
            // documents.filesAnalyzed is intentionally not part of analysis-report.json
            // — it
            // only surfaces in manifest.json's statistics.filesAnalyzed.
            new DocumentsPayload(report.documents().analyzed(), report.documents().ignored()),
            new RelationsPayload(report.relations().resolved(), report.relations().unresolved()),
            report.errors().stream()
                .map(e -> new ErrorPayload(e.project(), e.message(), e.category()))
                .toList());

    writeJsonFile(outputDirectory.resolve("analysis-report.json"), payload);
  }

  @Override
  public void writeSchema(Path outputDirectory) throws IOException {
    Files.writeString(
        outputDirectory.resolve("ciir.schema.json"), CiirSchemaProvider.getSchemaJson());
  }

  @Override
  public void writeManifest(AnalysisManifest manifest, Path outputDirectory) throws IOException {
    var payload =
        new ManifestPayload(
            "ciir",
            manifest.schemaVersion(),
            new GeneratorPayload("ciir-java", manifest.generatorVersion()),
            new InputPayload(manifest.inputType(), manifest.inputPath()),
            manifest.generatedAt(),
            manifest.projects(),
            manifest.files(),
            manifest.statistics());

    writeJsonFile(outputDirectory.resolve("manifest.json"), payload);
  }

  private void writeJsonFile(Path path, Object payload) throws IOException {
    try (var stream =
        Files.newOutputStream(
            path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      objectMapper.writeValue(stream, payload);
    }
  }

  private record ReportPayload(
      boolean success,
      ProjectsPayload projects,
      DocumentsPayload documents,
      RelationsPayload relations,
      List<ErrorPayload> errors) {}

  private record ProjectsPayload(int discovered, int analyzed, int failed) {}

  private record DocumentsPayload(int analyzed, int ignored) {}

  private record RelationsPayload(int resolved, int unresolved) {}

  private record ErrorPayload(String project, String message, String category) {}

  private record ManifestPayload(
      String format,
      String schemaVersion,
      GeneratorPayload generator,
      InputPayload input,
      Instant generatedAt,
      List<AnalysisManifest.ManifestProject> projects,
      List<AnalysisManifest.ManifestFile> files,
      AnalysisManifest.ManifestStatistics statistics) {}

  private record GeneratorPayload(String name, String version) {}

  private record InputPayload(InputType type, String path) {}
}
