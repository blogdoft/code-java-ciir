package dev.ftathiago.ciir.application.usecases;

import dev.ftathiago.ciir.application.discovery.ProjectDiscoveryService;
import dev.ftathiago.ciir.application.exceptions.AnalysisException;
import dev.ftathiago.ciir.application.exceptions.InvalidInputException;
import dev.ftathiago.ciir.application.input.PomInspector;
import dev.ftathiago.ciir.application.model.AnalysisExitCode;
import dev.ftathiago.ciir.application.model.AnalysisInput;
import dev.ftathiago.ciir.application.model.AnalysisManifest;
import dev.ftathiago.ciir.application.model.AnalysisManifest.ManifestFile;
import dev.ftathiago.ciir.application.model.AnalysisManifest.ManifestProject;
import dev.ftathiago.ciir.application.model.AnalysisManifest.ManifestStatistics;
import dev.ftathiago.ciir.application.model.AnalysisOptions;
import dev.ftathiago.ciir.application.model.AnalysisReport;
import dev.ftathiago.ciir.application.model.AnalysisResult;
import dev.ftathiago.ciir.application.model.InputType;
import dev.ftathiago.ciir.application.ports.AnalysisArtifactWriter;
import dev.ftathiago.ciir.application.ports.AnalysisProgressReporter;
import dev.ftathiago.ciir.application.ports.AnalysisReporter;
import dev.ftathiago.ciir.application.ports.CiirWriter;
import dev.ftathiago.ciir.application.ports.CiirWriterFactory;
import dev.ftathiago.ciir.application.ports.CodeAnalyzer;
import dev.ftathiago.ciir.application.ports.InputResolver;
import dev.ftathiago.ciir.core.hashing.Sha256Text;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * The main use case: {@code path} in, four artifact files out. This is the single orchestration
 * point the CLI (and, in the future, any other entry point) calls into — it has no knowledge of
 * JavaParser or of the command line.
 */
public final class AnalyzeInputHandler {

  private final InputResolver inputResolver;
  private final ProjectDiscoveryService projectDiscovery;
  private final CodeAnalyzer codeAnalyzer;
  private final CiirWriterFactory writerFactory;
  private final AnalysisArtifactWriter artifactWriter;
  private final AnalysisReporter reporter;
  private final AnalysisProgressReporter progress;

  public AnalyzeInputHandler(
      InputResolver inputResolver,
      ProjectDiscoveryService projectDiscovery,
      CodeAnalyzer codeAnalyzer,
      CiirWriterFactory writerFactory,
      AnalysisArtifactWriter artifactWriter,
      AnalysisReporter reporter,
      AnalysisProgressReporter progress) {
    this.inputResolver = inputResolver;
    this.projectDiscovery = projectDiscovery;
    this.codeAnalyzer = codeAnalyzer;
    this.writerFactory = writerFactory;
    this.artifactWriter = artifactWriter;
    this.reporter = reporter;
    this.progress = progress;
  }

  public AnalysisResult execute(AnalyzeInputCommand command) {
    return execute(command, () -> false);
  }

  /**
   * @param cancellationRequested polled between projects; when it turns {@code true}, a {@link
   *     CancellationException} propagates out of this method (never swallowed — a project failure
   *     and a cancellation are different things).
   */
  public AnalysisResult execute(
      AnalyzeInputCommand command, BooleanSupplier cancellationRequested) {
    var options = command.options();

    AnalysisInput input;
    try {
      input = inputResolver.resolve(command.path());
    } catch (InvalidInputException e) {
      return AnalysisResult.failure(AnalysisExitCode.INVALID_INPUT, e.getMessage());
    }

    var rootDirectory =
        input.type() == InputType.DIRECTORY ? input.path() : input.path().getParent();

    List<Path> projects;
    try {
      projects = projectDiscovery.discover(input);
    } catch (IOException e) {
      return AnalysisResult.failure(
          AnalysisExitCode.INVALID_INPUT, "Failed to discover projects: " + e.getMessage());
    }

    progress.onProjectsDiscovered(projects.size());
    projects.forEach(reporter::recordProjectDiscovered);

    try {
      Files.createDirectories(options.outputPath());
    } catch (IOException e) {
      return AnalysisResult.failure(AnalysisExitCode.OUTPUT_WRITE_FAILURE, e.getMessage());
    }

    boolean hasFatalProjectFailure;
    try {
      hasFatalProjectFailure =
          analyzeProjects(projects, rootDirectory, options, cancellationRequested);
    } catch (IOException e) {
      return AnalysisResult.failure(AnalysisExitCode.OUTPUT_WRITE_FAILURE, e.getMessage());
    }

    var report = reporter.buildReport();

    try {
      writeArtifacts(report, options, projects, input);
    } catch (IOException e) {
      return AnalysisResult.outputWriteFailure(e.getMessage(), report);
    }

    var exitCode =
        hasFatalProjectFailure && options.failOnError()
            ? AnalysisExitCode.FAILURE
            : AnalysisExitCode.SUCCESS;
    return AnalysisResult.of(exitCode, report);
  }

  private boolean analyzeProjects(
      List<Path> projects,
      Path rootDirectory,
      AnalysisOptions options,
      BooleanSupplier cancellationRequested)
      throws IOException {
    var hasFatalFailure = false;

    try (CiirWriter writer = writerFactory.create(options.outputPath())) {
      for (int i = 0; i < projects.size(); i++) {
        if (cancellationRequested.getAsBoolean()) {
          throw new CancellationException("Analysis was cancelled");
        }

        var project = projects.get(i);
        progress.onProjectStarted(project, i + 1, projects.size());
        var documentCount = new int[1];

        try {
          codeAnalyzer.analyze(
              project,
              rootDirectory,
              projects,
              options,
              document -> {
                reporter.recordDocument(document);
                try {
                  writer.write(document);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
                documentCount[0]++;
              });
          reporter.recordProjectAnalyzed(project);
          progress.onProjectCompleted(project, documentCount[0]);
        } catch (AnalysisException e) {
          reporter.recordProjectFailed(project, e.getMessage(), "project_load");
          progress.onProjectFailed(project, e.getMessage());
          hasFatalFailure = true;
        }
      }
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }

    return hasFatalFailure;
  }

  private void writeArtifacts(
      AnalysisReport report, AnalysisOptions options, List<Path> projects, AnalysisInput input)
      throws IOException {
    var outputDirectory = options.outputPath();
    artifactWriter.writeReport(report, outputDirectory);
    artifactWriter.writeSchema(outputDirectory);
    artifactWriter.writeManifest(
        buildManifest(report, projects, input, outputDirectory), outputDirectory);
  }

  private AnalysisManifest buildManifest(
      AnalysisReport report, List<Path> projects, AnalysisInput input, Path outputDirectory)
      throws IOException {
    var manifestProjects = new ArrayList<ManifestProject>();
    for (var project : projects) {
      manifestProjects.add(
          new ManifestProject(PomInspector.readArtifactId(project), project.toString()));
    }

    var jsonlPath = outputDirectory.resolve("ciir.jsonl");
    var schemaPath = outputDirectory.resolve("ciir.schema.json");
    var files =
        List.of(
            new ManifestFile("ciir.jsonl", report.documents().analyzed(), hashFile(jsonlPath)),
            new ManifestFile("ciir.schema.json", null, hashFile(schemaPath)));

    var statistics =
        new ManifestStatistics(
            report.projects().analyzed(),
            report.documents().filesAnalyzed(),
            report.entities().types(),
            report.entities().methods(),
            report.relations().resolved() + report.relations().unresolved(),
            report.relations().unresolved());

    return new AnalysisManifest(
        dev.ftathiago.ciir.core.SchemaVersion.CURRENT,
        generatorVersion(),
        input.type(),
        input.path().toString(),
        Instant.now(),
        manifestProjects,
        files,
        statistics);
  }

  private static String hashFile(Path path) throws IOException {
    return Sha256Text.computePrefixedHash(Files.readAllBytes(path));
  }

  private static String generatorVersion() {
    var version = AnalyzeInputHandler.class.getPackage().getImplementationVersion();
    return version != null ? version : "0.0.0";
  }
}
