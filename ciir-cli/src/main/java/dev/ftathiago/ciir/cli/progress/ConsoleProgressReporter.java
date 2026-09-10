package dev.ftathiago.ciir.cli.progress;

import dev.ftathiago.ciir.application.input.PomInspector;
import dev.ftathiago.ciir.application.ports.AnalysisProgressReporter;
import java.io.IOException;
import java.nio.file.Path;

/** The only class allowed to write to {@code System.out}/{@code System.err} in this project. */
public final class ConsoleProgressReporter implements AnalysisProgressReporter {

  @Override
  public void onProjectsDiscovered(int projectCount) {
    System.out.println("Discovering projects...");
    System.out.println("Found " + projectCount + " project(s).");
    System.out.println();
  }

  @Override
  public void onProjectStarted(Path projectPath, int index, int total) {
    System.out.println("[" + index + "/" + total + "] " + nameOf(projectPath));
  }

  @Override
  public void onProjectCompleted(Path projectPath, int documentCount) {
    System.out.println("       " + documentCount + " entities");
  }

  @Override
  public void onProjectFailed(Path projectPath, String message) {
    System.err.println("       failed: " + message);
  }

  private static String nameOf(Path projectPath) {
    try {
      return PomInspector.readArtifactId(projectPath);
    } catch (IOException e) {
      return projectPath.toString();
    }
  }
}
