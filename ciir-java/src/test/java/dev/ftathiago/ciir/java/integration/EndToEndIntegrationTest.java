package dev.ftathiago.ciir.java.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import dev.ftathiago.ciir.application.discovery.ProjectDiscoveryService;
import dev.ftathiago.ciir.application.input.DefaultInputResolver;
import dev.ftathiago.ciir.application.model.AnalysisExitCode;
import dev.ftathiago.ciir.application.model.AnalysisOptions;
import dev.ftathiago.ciir.application.ports.AnalysisArtifactWriter;
import dev.ftathiago.ciir.application.ports.CodeAnalyzer;
import dev.ftathiago.ciir.application.ports.InputResolver;
import dev.ftathiago.ciir.application.reporting.DefaultAnalysisReporter;
import dev.ftathiago.ciir.application.reporting.NullAnalysisProgressReporter;
import dev.ftathiago.ciir.application.usecases.AnalyzeInputCommand;
import dev.ftathiago.ciir.application.usecases.AnalyzeInputHandler;
import dev.ftathiago.ciir.java.analysis.JavaCodeAnalyzer;
import dev.ftathiago.ciir.serialization.schema.CiirSchemaProvider;
import dev.ftathiago.ciir.serialization.writing.DefaultAnalysisArtifactWriter;
import dev.ftathiago.ciir.serialization.writing.JsonlCiirWriterFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the real pipeline the CLI uses — {@link DefaultInputResolver} + {@link
 * ProjectDiscoveryService} + the real {@link JavaCodeAnalyzer} (Maven classpath resolution
 * included) + the real serialization writers — against the checked-in fixtures, validating every
 * emitted line against {@code ciir.schema.json} and checking end-to-end determinism.
 */
class EndToEndIntegrationTest {

  private static final Path REPO_ROOT = Path.of("").toAbsolutePath().getParent();
  private static final Path BASIC_PROJECT_POM = REPO_ROOT.resolve("fixtures/basic-project/pom.xml");
  private static final Path MULTI_PROJECT_AGGREGATOR =
      REPO_ROOT.resolve("fixtures/multiple-projects/pom.xml");

  private static JsonSchema schema;

  @BeforeAll
  static void setUp() throws Exception {
    schema =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(CiirSchemaProvider.getSchemaJson(), new SchemaValidatorsConfig());
    // fixtures/multiple-projects/application depends on fixtures/multiple-projects/domain as a
    // regular (non-reactor) Maven dependency; install it once so `mvn
    // dependency:build-classpath`
    // can resolve it, mirroring how a real multi-module repository is expected to already
    // build.
    installReactor(MULTI_PROJECT_AGGREGATOR);
  }

  @Test
  void basicProject_analyzesSuccessfullyAndProducesSchemaValidJsonl(@TempDir Path outputDir)
      throws Exception {
    var result = run(BASIC_PROJECT_POM.toString(), outputDir);

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.SUCCESS);
    var lines = validateEveryLine(outputDir.resolve("ciir.jsonl"));
    assertThat(lines).isNotEmpty();
    assertThat(lines)
        .anySatisfy(line -> assertThat(line).contains("\"kind\":\"method\"", "\"authorize\""));
    assertThat(lines).anySatisfy(line -> assertThat(line).contains("\"kind\":\"constructor\""));
    assertThat(lines).anySatisfy(line -> assertThat(line).contains("\"kind\":\"field\""));
    assertThat(lines).anySatisfy(line -> assertThat(line).contains("\"conditions\""));
    assertThat(lines).anySatisfy(line -> assertThat(line).contains("\"kind\":\"throws\""));
  }

  @Test
  void multipleProjects_crossModuleRelationsAreResolvedAsSolution(@TempDir Path outputDir)
      throws Exception {
    var result = run(MULTI_PROJECT_AGGREGATOR.toString(), outputDir);

    assertThat(result.exitCode()).isEqualTo(AnalysisExitCode.SUCCESS);
    var lines = validateEveryLine(outputDir.resolve("ciir.jsonl"));
    assertThat(lines)
        .anySatisfy(
            line ->
                assertThat(line)
                    .contains("\"origin\":\"solution\"")
                    .contains("\"status\":\"resolved\""));
  }

  @Test
  void analysis_isByteForByteDeterministic(@TempDir Path outputDir1, @TempDir Path outputDir2)
      throws Exception {
    run(BASIC_PROJECT_POM.toString(), outputDir1);
    run(BASIC_PROJECT_POM.toString(), outputDir2);

    assertThat(Files.readString(outputDir1.resolve("ciir.jsonl")))
        .isEqualTo(Files.readString(outputDir2.resolve("ciir.jsonl")));
  }

  private static dev.ftathiago.ciir.application.model.AnalysisResult run(
      String path, Path outputDir) {
    InputResolver inputResolver = new DefaultInputResolver();
    var discovery = new ProjectDiscoveryService();
    CodeAnalyzer codeAnalyzer = new JavaCodeAnalyzer();
    var writerFactory = new JsonlCiirWriterFactory();
    AnalysisArtifactWriter artifactWriter = new DefaultAnalysisArtifactWriter();
    var reporter = new DefaultAnalysisReporter();

    var handler =
        new AnalyzeInputHandler(
            inputResolver,
            discovery,
            codeAnalyzer,
            writerFactory,
            artifactWriter,
            reporter,
            NullAnalysisProgressReporter.INSTANCE);

    var options = new AnalysisOptions(outputDir, false, true, false, false);
    return handler.execute(new AnalyzeInputCommand(path, options));
  }

  private static List<String> validateEveryLine(Path jsonlPath) throws IOException {
    var lines =
        Files.readAllLines(jsonlPath, StandardCharsets.UTF_8).stream()
            .filter(l -> !l.isBlank())
            .toList();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    for (var line : lines) {
      var errors = schema.validate(mapper.readTree(line));
      assertThat(errors).as("schema violations for: %s", line).isEmpty();
    }
    return lines;
  }

  private static void installReactor(Path aggregatorPom) throws IOException, InterruptedException {
    var offline =
        new ProcessBuilder(
                "mvn", "-q", "-o", "-DskipTests", "install", "-f", aggregatorPom.toString())
            .redirectErrorStream(true)
            .start();
    var offlineOutput = new String(offline.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (offline.waitFor() == 0) {
      return;
    }
    var online =
        new ProcessBuilder("mvn", "-q", "-DskipTests", "install", "-f", aggregatorPom.toString())
            .redirectErrorStream(true)
            .start();
    var onlineOutput = new String(online.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (online.waitFor() != 0) {
      throw new IOException(
          "Failed to install fixture reactor "
              + aggregatorPom
              + ":\n"
              + offlineOutput
              + "\n"
              + onlineOutput);
    }
  }
}
