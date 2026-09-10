package dev.ftathiago.ciir.application.reporting;

import dev.ftathiago.ciir.application.ports.AnalysisProgressReporter;
import java.nio.file.Path;

/** The {@code --no-progress} implementation: does nothing. */
public final class NullAnalysisProgressReporter implements AnalysisProgressReporter {

  public static final NullAnalysisProgressReporter INSTANCE = new NullAnalysisProgressReporter();

  private NullAnalysisProgressReporter() {}

  @Override
  public void onProjectsDiscovered(int projectCount) {}

  @Override
  public void onProjectStarted(Path projectPath, int index, int total) {}

  @Override
  public void onProjectCompleted(Path projectPath, int documentCount) {}

  @Override
  public void onProjectFailed(Path projectPath, String message) {}
}
