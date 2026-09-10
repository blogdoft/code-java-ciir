package dev.ftathiago.ciir.java.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import dev.ftathiago.ciir.application.exceptions.AnalysisException;
import dev.ftathiago.ciir.application.input.PomInspector;
import dev.ftathiago.ciir.application.model.AnalysisOptions;
import dev.ftathiago.ciir.application.ports.CodeAnalyzer;
import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.java.classpath.MavenModuleClasspathResolver;
import dev.ftathiago.ciir.java.embeddingtext.JavaNoiseEmbeddingTextPolicy;
import dev.ftathiago.ciir.java.relations.RelationExtractor;
import dev.ftathiago.ciir.java.relations.ResolutionClassifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CodeAnalyzer} implementation for Java: JavaParser for syntax, its Symbol Solver for
 * semantics, driven by each Maven module's own resolved dependency classpath. The only class in
 * this reactor allowed to import {@code com.github.javaparser.*}.
 */
public final class JavaCodeAnalyzer implements CodeAnalyzer {

  private static final Logger LOG = LoggerFactory.getLogger(JavaCodeAnalyzer.class);
  private static final JavaNoiseEmbeddingTextPolicy EMBEDDING_TEXT_POLICY =
      new JavaNoiseEmbeddingTextPolicy();

  private final MavenModuleClasspathResolver classpathResolver = new MavenModuleClasspathResolver();

  // Built once per run (allProjectPaths is invariant across calls within a single run — see
  // CodeAnalyzer#analyze's contract). A JavaParser TypeSolver may only ever belong to a single
  // parent CombinedTypeSolver for its whole lifetime, so the solvers themselves cannot be
  // cached/reused across projects — only the (cheap, immutable) source-root-to-project mapping
  // is.
  private Map<Path, String> sourceRootToProjectName;

  @Override
  public boolean canAnalyze(Path projectPath) {
    return "pom.xml".equals(projectPath.getFileName().toString());
  }

  @Override
  public void analyze(
      Path projectPath,
      Path rootDirectory,
      List<Path> allProjectPaths,
      AnalysisOptions options,
      Consumer<CiirDocument> sink)
      throws AnalysisException {
    try {
      ensureSourceRootMappingBuilt(allProjectPaths);

      var projectName = PomInspector.readArtifactId(projectPath);
      var moduleSourceRoot = sourceRootOf(projectPath);
      if (!Files.isDirectory(moduleSourceRoot)) {
        LOG.info("Module {} has no src/main/java; nothing to analyze.", projectName);
        return;
      }

      // A fresh CombinedTypeSolver (and fresh child solvers) every call: a TypeSolver may
      // only ever have one parent for its lifetime, so nothing here can be cached/reused
      // across projects within the same run — only sourceRootToProjectName is.
      //
      // The ParserConfiguration is built (minus its symbol resolver) BEFORE the
      // JavaParserTypeSolvers, and the same mutable instance is handed to each one: a
      // JavaParserTypeSolver parses files it discovers with its OWN internal JavaParser, and
      // without this shared configuration those internal parses would have no symbol
      // resolver at all — resolution into another module's source (e.g. a record accessor)
      // would fail with "Symbol resolution not configured". Setting the resolver on this
      // same instance afterwards (once the solver exists) fixes that for every consumer.
      var parserConfiguration =
          new ParserConfiguration()
              .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);

      var solver = new CombinedTypeSolver();
      // ReflectionTypeSolver's no-arg constructor defaults to a JRE-only filter that only
      // recognizes "java.*"-prefixed classes — it wrongly excludes plenty of legitimate JDK
      // classes outside that prefix (org.w3c.dom.*, org.xml.sax.*, javax.*), which then fail
      // to resolve as "unsolved" even though Class.forName finds them fine. Passing `false`
      // disables that restriction.
      solver.add(new ReflectionTypeSolver(false));
      for (var sourceRoot : sourceRootToProjectName.keySet()) {
        solver.add(new JavaParserTypeSolver(sourceRoot, parserConfiguration));
      }
      for (var jar : classpathResolver.resolve(projectPath)) {
        if (Files.isRegularFile(jar)) {
          solver.add(new JarTypeSolver(jar));
        }
      }
      parserConfiguration.setSymbolResolver(new JavaSymbolSolver(solver));

      var parser = new JavaParser(parserConfiguration);

      var classifier = new ResolutionClassifier(sourceRootToProjectName, moduleSourceRoot);
      var walker =
          new CompilationUnitWalker(
              projectName,
              rootDirectory,
              new RelationExtractor(classifier),
              EMBEDDING_TEXT_POLICY,
              options.includeSource(),
              new HashSet<>());

      try (var javaFiles = Files.walk(moduleSourceRoot)) {
        for (var file : javaFiles.filter(p -> p.toString().endsWith(".java")).toList()) {
          var parseResult = parser.parse(file);
          var compilationUnit = parseResult.getResult().orElse(null);
          if (compilationUnit == null) {
            throw new AnalysisException(
                "Failed to parse " + file + ": " + parseResult.getProblems());
          }
          walker.walk(compilationUnit, file, sink);
        }
      }
    } catch (IOException e) {
      throw new AnalysisException("Failed to analyze " + projectPath + ": " + e.getMessage(), e);
    }
  }

  private void ensureSourceRootMappingBuilt(List<Path> allProjectPaths) throws IOException {
    if (sourceRootToProjectName != null) {
      return;
    }

    var mapping = new HashMap<Path, String>();
    for (var pomPath : allProjectPaths) {
      var sourceRoot = sourceRootOf(pomPath);
      if (Files.isDirectory(sourceRoot)) {
        mapping.put(sourceRoot, PomInspector.readArtifactId(pomPath));
      }
    }
    sourceRootToProjectName = Map.copyOf(mapping);
  }

  private static Path sourceRootOf(Path pomPath) {
    return pomPath.getParent().resolve("src/main/java").toAbsolutePath().normalize();
  }
}
