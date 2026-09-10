package dev.ftathiago.ciir.application.ports;

import dev.ftathiago.ciir.application.exceptions.AnalysisException;
import dev.ftathiago.ciir.application.model.AnalysisOptions;
import dev.ftathiago.ciir.core.CiirDocument;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * A language-specific analysis backend. {@code ciir-application} depends only on this abstraction,
 * never on JavaParser directly.
 */
public interface CodeAnalyzer {

  boolean canAnalyze(Path projectPath);

  /**
   * Analyzes {@code projectPath}, pushing one {@link CiirDocument} at a time to {@code sink} as
   * they are produced — never buffering a whole project's documents in memory.
   *
   * @param rootDirectory the original resolved analysis root every emitted {@code source.path} is
   *     computed relative to (not necessarily {@code projectPath}'s own directory).
   * @param allProjectPaths every project path discovered in this run (including {@code projectPath}
   *     itself), invariant across every call within one run — lets the analyzer resolve relations
   *     into a sibling module (@code resolution.origin: "solution"}) regardless of analysis order.
   */
  void analyze(
      Path projectPath,
      Path rootDirectory,
      List<Path> allProjectPaths,
      AnalysisOptions options,
      Consumer<CiirDocument> sink)
      throws AnalysisException;
}
