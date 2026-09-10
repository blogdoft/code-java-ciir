package dev.ftathiago.ciir.application.ports;

import dev.ftathiago.ciir.application.model.AnalysisManifest;
import dev.ftathiago.ciir.application.model.AnalysisReport;
import java.io.IOException;
import java.nio.file.Path;

public interface AnalysisArtifactWriter {

  void writeReport(AnalysisReport report, Path outputDirectory) throws IOException;

  /** Copies the packaged {@code ciir.schema.json} verbatim into {@code outputDirectory}. */
  void writeSchema(Path outputDirectory) throws IOException;

  void writeManifest(AnalysisManifest manifest, Path outputDirectory) throws IOException;
}
