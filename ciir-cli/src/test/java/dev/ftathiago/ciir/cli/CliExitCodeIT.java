package dev.ftathiago.ciir.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spawns the actual built {@code ciir-cli.jar} as a real OS subprocess and asserts on its exit code
 * and stderr — genuine black-box CLI testing, not an in-process call to {@link Main}. Runs in the
 * {@code integration-test} phase (after {@code package}, when the jar actually exists).
 */
class CliExitCodeIT {

  private static final Path JAR_PATH = Path.of("target/ciir-cli.jar").toAbsolutePath();
  private static final Path REPO_ROOT = Path.of("").toAbsolutePath().getParent();

  @Test
  void nonexistentPath_exitsWithInvalidInputAndReportsIt() throws Exception {
    var result = runCli("/does/not/exist/pom.xml");

    assertThat(result.exitCode()).isEqualTo(2);
    assertThat(result.stderr()).contains("does not exist");
  }

  @Test
  void missingRequiredPathArgument_exitsWithInvalidInput() throws Exception {
    var result = runCli();

    assertThat(result.exitCode()).isEqualTo(2);
  }

  @Test
  void unsupportedFileExtension_exitsWithInvalidInput(@TempDir Path tempDir) throws Exception {
    var file = tempDir.resolve("readme.md");
    Files.writeString(file, "not a pom");

    var result = runCli(file.toString());

    assertThat(result.exitCode()).isEqualTo(2);
  }

  @Test
  void basicProjectFixture_exitsSuccessfullyAndWritesArtifacts(@TempDir Path outputDir)
      throws Exception {
    var pom = REPO_ROOT.resolve("fixtures/basic-project/pom.xml").toString();

    var result = runCli(pom, "--output", outputDir.toString(), "--no-progress");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(outputDir.resolve("ciir.jsonl")).exists();
    assertThat(outputDir.resolve("manifest.json")).exists();
  }

  private record CliResult(int exitCode, String stdout, String stderr) {}

  private static CliResult runCli(String... args) throws IOException, InterruptedException {
    var command = new java.util.ArrayList<String>();
    command.add("java");
    command.add("-jar");
    command.add(JAR_PATH.toString());
    command.addAll(java.util.List.of(args));

    var process = new ProcessBuilder(command).start();
    var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    var exitCode = process.waitFor();
    return new CliResult(exitCode, stdout, stderr);
  }
}
