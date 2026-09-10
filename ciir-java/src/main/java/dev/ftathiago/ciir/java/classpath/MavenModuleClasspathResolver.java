package dev.ftathiago.ciir.java.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves a Maven module's dependency classpath by shelling out to {@code mvn
 * dependency:build-classpath} — the direct analogue of the C# generator's {@code MSBuildWorkspace}
 * resolving via MSBuild/NuGet, and far simpler/more reliable than re-implementing Maven dependency
 * resolution by hand. Requires a working {@code mvn} on {@code PATH} and, for not-yet-resolved
 * dependencies, network access to populate {@code ~/.m2}.
 */
public final class MavenModuleClasspathResolver {

  public List<Path> resolve(Path pomPath) throws IOException {
    var outputFile = Files.createTempFile("ciir-classpath-", ".txt");
    try {
      runMaven(pomPath, outputFile, true);
      var content = Files.readString(outputFile, StandardCharsets.UTF_8).strip();
      if (content.isEmpty()) {
        return List.of();
      }
      return Arrays.stream(content.split(File.pathSeparator)).map(Path::of).toList();
    } finally {
      Files.deleteIfExists(outputFile);
    }
  }

  private void runMaven(Path pomPath, Path outputFile, boolean offlineFirst) throws IOException {
    var offlineResult = offlineFirst ? execute(pomPath, outputFile, true) : null;
    if (offlineResult != null && offlineResult.success()) {
      return;
    }

    var onlineResult = execute(pomPath, outputFile, false);
    if (!onlineResult.success()) {
      throw new IOException(
          "mvn dependency:build-classpath failed for " + pomPath + ":\n" + onlineResult.output());
    }
  }

  private MavenResult execute(Path pomPath, Path outputFile, boolean offline) throws IOException {
    var command = new java.util.ArrayList<String>(List.of("mvn", "-q"));
    if (offline) {
      command.add("-o");
    }
    command.add("dependency:build-classpath");
    command.add("-Dmdep.outputFile=" + outputFile);
    command.add("-f");
    command.add(pomPath.toString());

    try {
      var process = new ProcessBuilder(command).redirectErrorStream(true).start();
      var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      var exitCode = process.waitFor();
      return new MavenResult(exitCode == 0, output);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while resolving classpath for " + pomPath, e);
    }
  }

  private record MavenResult(boolean success, String output) {}
}
