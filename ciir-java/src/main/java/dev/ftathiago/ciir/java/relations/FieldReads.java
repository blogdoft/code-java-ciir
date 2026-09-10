package dev.ftathiago.ciir.java.relations;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import dev.ftathiago.ciir.java.symbolnaming.SymbolNaming;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves every {@code NameExpr}/{@code FieldAccessExpr} within an expression that statically
 * resolves to a field, returning each as a fully qualified symbol string. Shared between {@code
 * ConditionExtractor} (the {@code condition.reads} list) and {@code RelationExtractor} (the {@code
 * reads} relation kind).
 */
public final class FieldReads {

  private FieldReads() {}

  public static List<String> collect(Expression expression) {
    var reads = new ArrayList<String>();
    for (var nameExpr : expression.findAll(NameExpr.class)) {
      resolveField(nameExpr::resolve).ifPresent(reads::add);
    }
    for (var fieldAccessExpr : expression.findAll(FieldAccessExpr.class)) {
      resolveField(fieldAccessExpr::resolve).ifPresent(reads::add);
    }
    return reads;
  }

  private static Optional<String> resolveField(Supplier<ResolvedValueDeclaration> resolver) {
    try {
      var resolved = resolver.get();
      return resolved.isField()
          ? Optional.of(SymbolNaming.qualifiedName(resolved.asField()))
          : Optional.empty();
    } catch (RuntimeException e) {
      // Resolution genuinely fails for plenty of legitimate reasons (local variables,
      // unresolvable generics, ...) — best-effort collection, never fatal.
      return Optional.empty();
    }
  }
}
