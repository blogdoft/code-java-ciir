package dev.ftathiago.ciir.java.relations;

import com.github.javaparser.resolution.declarations.AssociableToAST;
import dev.ftathiago.ciir.core.relations.CiirResolutionOrigin;
import dev.ftathiago.ciir.core.relations.CiirResolutionStatus;
import java.nio.file.Path;
import java.util.Map;

/**
 * Decides a successfully-resolved symbol's {@code resolution.status}/{@code origin} and, when
 * relevant, which analyzed Maven module owns it (needed to compute {@code target.id}).
 *
 * <p>Mechanism: {@link AssociableToAST#toAst()} tells us whether the symbol solver resolved the
 * symbol from source ({@code JavaParserTypeSolver}) or from bytecode ({@code
 * ReflectionTypeSolver}/{@code JarTypeSolver}) — JavaParser exposes no other official way to ask
 * "which type solver resolved this". For the source case, the resolved declaration's {@code
 * CompilationUnit}'s {@code Storage.getSourceRoot()} tells us exactly which registered module it
 * belongs to. For the bytecode case, the resolved declaration's own implementation class package
 * name (a JavaParser implementation detail, but a stable one across the 3.x line) distinguishes the
 * JDK ({@code reflectionmodel}) from a resolved jar dependency ({@code javassistmodel}).
 */
public final class ResolutionClassifier {

  private static final String REFLECTION_MODEL_PACKAGE =
      "com.github.javaparser.symbolsolver.reflectionmodel";
  private static final String JAVASSIST_MODEL_PACKAGE =
      "com.github.javaparser.symbolsolver.javassistmodel";

  private final Map<Path, String> sourceRootToProjectName;
  private final Path currentModuleSourceRoot;

  public ResolutionClassifier(
      Map<Path, String> sourceRootToProjectName, Path currentModuleSourceRoot) {
    this.sourceRootToProjectName = sourceRootToProjectName;
    this.currentModuleSourceRoot = currentModuleSourceRoot;
  }

  public record Classification(
      CiirResolutionStatus status, CiirResolutionOrigin origin, String owningProjectName) {

    public static Classification unknown() {
      return new Classification(CiirResolutionStatus.EXTERNAL, CiirResolutionOrigin.UNKNOWN, null);
    }
  }

  public Classification classify(AssociableToAST resolved) {
    var ast = resolved.toAst();
    if (ast.isEmpty()) {
      return classifyBytecodeOrigin(resolved);
    }

    var sourceRoot =
        ast.get()
            .findCompilationUnit()
            .flatMap(unit -> unit.getStorage())
            .map(storage -> storage.getSourceRoot().toAbsolutePath().normalize())
            .orElse(null);
    if (sourceRoot == null) {
      return Classification.unknown();
    }

    var owningProjectName = sourceRootToProjectName.get(sourceRoot);
    if (owningProjectName == null) {
      return Classification.unknown();
    }

    var origin =
        sourceRoot.equals(currentModuleSourceRoot)
            ? CiirResolutionOrigin.PROJECT
            : CiirResolutionOrigin.SOLUTION;
    return new Classification(CiirResolutionStatus.RESOLVED, origin, owningProjectName);
  }

  private static Classification classifyBytecodeOrigin(AssociableToAST resolved) {
    var packageName = resolved.getClass().getPackageName();
    if (packageName.equals(REFLECTION_MODEL_PACKAGE)) {
      return new Classification(
          CiirResolutionStatus.EXTERNAL, CiirResolutionOrigin.FRAMEWORK, null);
    }
    if (packageName.equals(JAVASSIST_MODEL_PACKAGE)) {
      return new Classification(
          CiirResolutionStatus.EXTERNAL, CiirResolutionOrigin.DEPENDENCY, null);
    }
    return Classification.unknown();
  }
}
