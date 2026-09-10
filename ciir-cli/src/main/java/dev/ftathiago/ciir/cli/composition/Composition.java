package dev.ftathiago.ciir.cli.composition;

import dev.ftathiago.ciir.application.discovery.ProjectDiscoveryService;
import dev.ftathiago.ciir.application.input.DefaultInputResolver;
import dev.ftathiago.ciir.application.ports.AnalysisProgressReporter;
import dev.ftathiago.ciir.application.reporting.DefaultAnalysisReporter;
import dev.ftathiago.ciir.application.reporting.NullAnalysisProgressReporter;
import dev.ftathiago.ciir.application.usecases.AnalyzeInputHandler;
import dev.ftathiago.ciir.java.analysis.JavaCodeAnalyzer;
import dev.ftathiago.ciir.serialization.writing.DefaultAnalysisArtifactWriter;
import dev.ftathiago.ciir.serialization.writing.JsonlCiirWriterFactory;

/**
 * The single wiring point referencing both {@code ciir-java} (the language adapter) and {@code
 * ciir-serialization} (the I/O adapter). Manual constructor wiring — no DI container is warranted
 * for this small a set of services.
 */
public final class Composition {

  private Composition() {}

  public static AnalyzeInputHandler buildHandler(
      boolean verbose, AnalysisProgressReporter progress) {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", verbose ? "debug" : "warn");

    return new AnalyzeInputHandler(
        new DefaultInputResolver(),
        new ProjectDiscoveryService(),
        new JavaCodeAnalyzer(),
        new JsonlCiirWriterFactory(),
        new DefaultAnalysisArtifactWriter(),
        new DefaultAnalysisReporter(),
        progress != null ? progress : NullAnalysisProgressReporter.INSTANCE);
  }
}
