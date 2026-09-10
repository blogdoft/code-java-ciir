package dev.ftathiago.ciir.application.discovery;

import dev.ftathiago.ciir.application.input.PomInspector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Given an aggregator {@code pom.xml} (one declaring {@code <modules>}), lists every leaf module
 * {@code pom.xml} it transitively references — the Maven analogue of listing a {@code .sln}'s
 * referenced {@code .csproj} files. A module that is itself an aggregator is expanded rather than
 * returned (only leaf module POMs are analyzable projects).
 *
 * <p>This needs no Java-source-analysis dependency (it is plain XML/Maven-model reading), so unlike
 * the C# generator's {@code ISolutionProjectLister} it lives in {@code ciir-application} itself
 * rather than in the language-specific analyzer module.
 */
public final class MavenModuleLister {

  private MavenModuleLister() {}

  public static List<Path> listProjectPaths(Path aggregatorPom) throws IOException {
    var result = new LinkedHashSet<Path>();
    collect(aggregatorPom.toAbsolutePath().normalize(), result, new LinkedHashSet<>());
    return List.copyOf(result);
  }

  private static void collect(Path pomPath, Set<Path> result, Set<Path> visiting)
      throws IOException {
    if (!visiting.add(pomPath)) {
      throw new IOException("Circular <modules> reference detected at " + pomPath);
    }
    if (!Files.exists(pomPath)) {
      throw new IOException("Referenced module POM does not exist: " + pomPath);
    }

    var modules = PomInspector.readModules(pomPath);
    if (modules.isEmpty()) {
      result.add(pomPath);
      return;
    }

    var parentDirectory = pomPath.getParent();
    for (var module : modules) {
      collect(resolveModulePom(parentDirectory, module), result, visiting);
    }
  }

  private static Path resolveModulePom(Path parentDirectory, String module) {
    var resolved = parentDirectory.resolve(module).normalize();
    if (Files.isDirectory(resolved)) {
      return resolved.resolve("pom.xml");
    }
    return resolved;
  }
}
