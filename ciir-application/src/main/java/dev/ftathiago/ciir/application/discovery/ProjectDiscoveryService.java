package dev.ftathiago.ciir.application.discovery;

import dev.ftathiago.ciir.application.input.PomInspector;
import dev.ftathiago.ciir.application.model.AnalysisInput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resolves an {@link AnalysisInput} into the unique, deterministically ordered set of leaf module
 * {@code pom.xml} paths to analyze. A concrete class, not an interface — there is no plausible
 * second implementation.
 */
public final class ProjectDiscoveryService {

  /**
   * Directory basenames never descended into. Maven/IDE equivalents of the C# generator's {@code
   * bin}/{@code obj}/{@code .git}/{@code .vs}. Matched by exact basename, case-sensitive (Linux
   * filesystems are case-sensitive, unlike the C# generator's Windows-flavored choice).
   */
  private static final Set<String> IGNORED_DIR_NAMES =
      Set.of("target", ".git", ".idea", ".settings");

  public List<Path> discover(AnalysisInput input) throws IOException {
    var projectPaths =
        switch (input.type()) {
          case PROJECT -> List.of(input.path().toAbsolutePath().normalize());
          case AGGREGATOR -> MavenModuleLister.listProjectPaths(input.path());
          case DIRECTORY -> discoverInDirectory(input.path());
        };

    // A leaf pom.xml found both directly during a directory walk AND via some aggregator's
    // <modules> resolution collapses to one entry here (same normalized absolute path = same
    // set key) — no special-case dedup logic needed beyond "it's all one set".
    return List.copyOf(new TreeSet<>(projectPaths));
  }

  private static List<Path> discoverInDirectory(Path root) throws IOException {
    var result = new LinkedHashSet<Path>();
    Deque<Path> stack = new ArrayDeque<>();
    stack.push(root.toAbsolutePath().normalize());

    while (!stack.isEmpty()) {
      var current = stack.pop();
      List<Path> entries;
      try (var stream = Files.list(current)) {
        entries = stream.toList();
      }

      for (var entry : entries) {
        if (Files.isDirectory(entry)) {
          if (!IGNORED_DIR_NAMES.contains(entry.getFileName().toString())) {
            stack.push(entry);
          }
        } else if ("pom.xml".equals(entry.getFileName().toString())) {
          if (PomInspector.isAggregator(entry)) {
            result.addAll(MavenModuleLister.listProjectPaths(entry));
          } else {
            result.add(entry.toAbsolutePath().normalize());
          }
        }
      }
    }
    return List.copyOf(result);
  }
}
