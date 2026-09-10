package dev.ftathiago.ciir.application.ports;

import java.nio.file.Path;

/** Progress presentation, kept out of the analysis logic entirely. */
public interface AnalysisProgressReporter {

  void onProjectsDiscovered(int projectCount);

  /**
   * @param index 1-based.
   */
  void onProjectStarted(Path projectPath, int index, int total);

  void onProjectCompleted(Path projectPath, int documentCount);

  void onProjectFailed(Path projectPath, String message);
}
