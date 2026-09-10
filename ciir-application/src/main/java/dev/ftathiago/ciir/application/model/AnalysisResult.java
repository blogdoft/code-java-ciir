package dev.ftathiago.ciir.application.model;

import java.util.Objects;

/**
 * The outcome of {@code AnalyzeInputHandler.execute}.
 *
 * @param errorMessage set only for input-resolution/discovery/output-directory failures that happen
 *     before a {@link #report()} exists.
 * @param report {@code null} only for the early-failure paths above; populated (even on an
 *     artifact-write failure) once analysis has actually run.
 */
public record AnalysisResult(
    AnalysisExitCode exitCode, String errorMessage, AnalysisReport report) {

  public AnalysisResult {
    Objects.requireNonNull(exitCode, "exitCode");
  }

  public static AnalysisResult failure(AnalysisExitCode exitCode, String errorMessage) {
    return new AnalysisResult(exitCode, errorMessage, null);
  }

  public static AnalysisResult of(AnalysisExitCode exitCode, AnalysisReport report) {
    return new AnalysisResult(exitCode, null, report);
  }

  public static AnalysisResult outputWriteFailure(String errorMessage, AnalysisReport report) {
    return new AnalysisResult(AnalysisExitCode.OUTPUT_WRITE_FAILURE, errorMessage, report);
  }
}
