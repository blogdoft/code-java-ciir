package dev.ftathiago.ciir.application.exceptions;

/** The raw path given to the CLI does not resolve to a supported analysis input. */
public class InvalidInputException extends Exception {

  public InvalidInputException(String message) {
    super(message);
  }

  public InvalidInputException(String message, Throwable cause) {
    super(message, cause);
  }
}
