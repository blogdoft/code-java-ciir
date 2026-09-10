package dev.ftathiago.ciir.application.exceptions;

/** A single project/module failed to load or analyze. Never silently swallowed. */
public class AnalysisException extends Exception {

  public AnalysisException(String message) {
    super(message);
  }

  public AnalysisException(String message, Throwable cause) {
    super(message, cause);
  }
}
