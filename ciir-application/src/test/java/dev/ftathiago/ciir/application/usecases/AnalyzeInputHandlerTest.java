package dev.ftathiago.ciir.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.ftathiago.ciir.application.discovery.ProjectDiscoveryService;
import dev.ftathiago.ciir.application.exceptions.AnalysisException;
import dev.ftathiago.ciir.application.exceptions.InvalidInputException;
import dev.ftathiago.ciir.application.model.AnalysisExitCode;
import dev.ftathiago.ciir.application.model.AnalysisInput;
import dev.ftathiago.ciir.application.model.AnalysisOptions;
import dev.ftathiago.ciir.application.model.InputType;
import dev.ftathiago.ciir.application.ports.AnalysisArtifactWriter;
import dev.ftathiago.ciir.application.ports.AnalysisProgressReporter;
import dev.ftathiago.ciir.application.ports.AnalysisReporter;
import dev.ftathiago.ciir.application.ports.CiirWriter;
import dev.ftathiago.ciir.application.ports.CiirWriterFactory;
import dev.ftathiago.ciir.application.ports.CodeAnalyzer;
import dev.ftathiago.ciir.application.ports.InputResolver;
import dev.ftathiago.ciir.application.reporting.DefaultAnalysisReporter;
import dev.ftathiago.ciir.application.reporting.NullAnalysisProgressReporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzeInputHandlerTest {

  private final InputResolver inputResolver = mock(InputResolver.class);
  private final ProjectDiscoveryService discovery = mock(ProjectDiscoveryService.class);
  private final CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
  private final CiirWriterFactory writerFactory = mock(CiirWriterFactory.class);
  private final CiirWriter writer = mock(CiirWriter.class);
  private final AnalysisArtifactWriter artifactWriter = mock(AnalysisArtifactWriter.class);
  private final AnalysisReporter reporter = new DefaultAnalysisReporter();
  private final AnalysisProgressReporter progress = NullAnalysisProgressReporter.INSTANCE;

  private AnalyzeInputHandler handler;

  @BeforeEach
  void setUp() throws IOException {
    handler =
        new AnalyzeInputHandler(
            inputResolver,
            discovery,
            codeAnalyzer,
            writerFactory,
            artifactWriter,
            reporter,
            progress);
    when(writerFactory.create(any())).thenReturn(writer);
  }

  @Test
  void invalidInput_neverInvokesAnalyzer(@TempDir Path outputDir) throws Exception {
    when(inputResolver.resolve("bad")).thenThrow(new InvalidInputException("nope"));

    var result = handler.execute(new AnalyzeInputCommand("bad", options(outputDir)));

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.INVALID_INPUT);
    assertThat(result.errorMessage()).isEqualTo("nope");
    verifyNoInteractions(codeAnalyzer, writerFactory);
  }

  @Test
  void discoveryFailure_returnsInvalidInput(@TempDir Path outputDir) throws Exception {
    var pom = outputDir.resolve("pom.xml");
    when(inputResolver.resolve("p")).thenReturn(new AnalysisInput(InputType.PROJECT, pom));
    when(discovery.discover(any())).thenThrow(new IOException("boom"));

    var result = handler.execute(new AnalyzeInputCommand("p", options(outputDir)));

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.INVALID_INPUT);
    assertThat(result.errorMessage()).contains("boom");
  }

  @Test
  void projectFailureWithoutFailOnError_isStillSuccess(
      @TempDir Path outputDir, @TempDir Path moduleDir) throws Exception {
    var pom = writePom(moduleDir, "broken");
    stubDiscovery(pom);
    doThrow(new AnalysisException("could not load"))
        .when(codeAnalyzer)
        .analyze(any(), any(), any(), any(), any());
    stubArtifactWrites(outputDir);

    var result = handler.execute(new AnalyzeInputCommand(pom.toString(), options(outputDir)));

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.SUCCESS);
    assertThat(result.report().projects().failed()).isEqualTo(1);
  }

  @Test
  void projectFailureWithFailOnError_isFailure(@TempDir Path outputDir, @TempDir Path moduleDir)
      throws Exception {
    var pom = writePom(moduleDir, "broken");
    stubDiscovery(pom);
    doThrow(new AnalysisException("could not load"))
        .when(codeAnalyzer)
        .analyze(any(), any(), any(), any(), any());
    stubArtifactWrites(outputDir);

    var result = handler.execute(new AnalyzeInputCommand(pom.toString(), options(outputDir, true)));

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.FAILURE);
  }

  @Test
  void outputDirectoryCreationFailure_returnsOutputWriteFailure(@TempDir Path parent)
      throws Exception {
    var pom = parent.resolve("pom.xml");
    when(inputResolver.resolve(pom.toString()))
        .thenReturn(new AnalysisInput(InputType.PROJECT, pom));
    when(discovery.discover(any())).thenReturn(java.util.List.of(pom));
    // A file (not a directory) at the output path makes Files.createDirectories fail.
    var blockingFile = parent.resolve("blocked");
    Files.writeString(blockingFile, "x");
    var outputPath = blockingFile.resolve("nested");

    var result = handler.execute(new AnalyzeInputCommand(pom.toString(), options(outputPath)));

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.OUTPUT_WRITE_FAILURE);
  }

  private void stubDiscovery(Path pom) throws Exception {
    when(inputResolver.resolve(pom.toString()))
        .thenReturn(new AnalysisInput(InputType.PROJECT, pom));
    when(discovery.discover(any())).thenReturn(java.util.List.of(pom));
  }

  private void stubArtifactWrites(Path outputDir) throws IOException {
    doAnswer(
            invocation -> {
              Files.writeString(outputDir.resolve("ciir.jsonl"), "");
              return null;
            })
        .when(artifactWriter)
        .writeReport(any(), any());
    doAnswer(
            invocation -> {
              Files.writeString(outputDir.resolve("ciir.schema.json"), "{}");
              return null;
            })
        .when(artifactWriter)
        .writeSchema(any());
  }

  private static Path writePom(Path dir, String artifactId) throws IOException {
    var pom = dir.resolve("pom.xml");
    Files.writeString(
        pom,
        """
                <project><modelVersion>4.0.0</modelVersion>
                <artifactId>%s</artifactId></project>"""
            .formatted(artifactId));
    return pom;
  }

  private static AnalysisOptions options(Path outputDir) {
    return options(outputDir, false);
  }

  private static AnalysisOptions options(Path outputDir, boolean failOnError) {
    return new AnalysisOptions(outputDir, false, true, false, failOnError);
  }
}
