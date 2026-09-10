package dev.ftathiago.ciir.application.model;

public enum AnalysisExitCode {
  SUCCESS(0),
  FAILURE(1),
  INVALID_INPUT(2),
  OUTPUT_WRITE_FAILURE(3);

  private final int code;

  AnalysisExitCode(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }
}
