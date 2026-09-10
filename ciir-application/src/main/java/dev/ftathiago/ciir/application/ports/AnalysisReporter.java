package dev.ftathiago.ciir.application.ports;

import dev.ftathiago.ciir.application.model.AnalysisReport;
import dev.ftathiago.ciir.core.CiirDocument;
import java.nio.file.Path;

/** Accumulates the operational facts that become {@link AnalysisReport}. */
public interface AnalysisReporter {

  void recordProjectDiscovered(Path projectPath);

  void recordProjectAnalyzed(Path projectPath);

  void recordProjectFailed(Path projectPath, String message, String category);

  void recordDocument(CiirDocument document);

  AnalysisReport buildReport();
}
