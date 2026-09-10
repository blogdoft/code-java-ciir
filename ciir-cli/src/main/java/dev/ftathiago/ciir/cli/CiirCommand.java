package dev.ftathiago.ciir.cli;

import dev.ftathiago.ciir.application.model.AnalysisExitCode;
import dev.ftathiago.ciir.application.model.AnalysisOptions;
import dev.ftathiago.ciir.application.usecases.AnalyzeInputCommand;
import dev.ftathiago.ciir.cli.composition.Composition;
import dev.ftathiago.ciir.cli.progress.ConsoleProgressReporter;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "ciir",
    mixinStandardHelpOptions = true,
    description = "Statically analyzes Java source and produces CIIR (ciir.jsonl).")
public final class CiirCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "A Maven pom.xml (leaf module or aggregator), or a directory to scan.")
  private String path;

  @Option(
      names = "--output",
      description = "Output directory (default: ./ciir-output).",
      defaultValue = "./ciir-output")
  private String output;

  @Option(names = "--verbose", description = "Verbose diagnostic logging.")
  private boolean verbose;

  @Option(names = "--no-progress", description = "Suppress progress reporting.")
  private boolean noProgress;

  @Option(
      names = "--include-source",
      description = "Embed the literal source text of each entity's declaration.")
  private boolean includeSource;

  @Option(
      names = "--fail-on-error",
      description = "Exit with a non-zero code if any project fails to load/analyze.")
  private boolean failOnError;

  @Override
  public Integer call() {
    var options =
        new AnalysisOptions(
            Path.of(output).toAbsolutePath().normalize(),
            verbose,
            noProgress,
            includeSource,
            failOnError);
    var progress = noProgress ? null : new ConsoleProgressReporter();
    var handler = Composition.buildHandler(verbose, progress);

    var cancellationRequested = new AtomicBoolean(false);
    var shutdownHook = new Thread(() -> cancellationRequested.set(true));
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    try {
      var result =
          handler.execute(new AnalyzeInputCommand(path, options), cancellationRequested::get);

      if (result.errorMessage() != null) {
        System.err.println(result.errorMessage());
      }
      if (result.report() != null && !result.report().success()) {
        result
            .report()
            .errors()
            .forEach(
                error ->
                    System.err.println(
                        error.project() + ": " + error.message() + " (" + error.category() + ")"));
      }
      return result.exitCode().code();
    } catch (java.util.concurrent.CancellationException e) {
      System.err.println("Analysis cancelled.");
      return AnalysisExitCode.FAILURE.code();
    } finally {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
  }
}
