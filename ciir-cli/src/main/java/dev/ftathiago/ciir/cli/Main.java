package dev.ftathiago.ciir.cli;

import picocli.CommandLine;

/**
 * The composition root and command-line entry point. Contains no analysis logic itself: parses
 * arguments, wires the concrete adapters ({@link dev.ftathiago.ciir.cli.composition.Composition}),
 * and calls into {@code ciir-application}.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    // picocli's default parameter-error handling already prints the problem + usage to
    // stderr and returns CommandLine.ExitCode.USAGE (2) without invoking call() — exactly
    // this project's "invalid arguments/input" exit code (see AnalysisExitCode).
    System.exit(new CommandLine(new CiirCommand()).execute(args));
  }
}
