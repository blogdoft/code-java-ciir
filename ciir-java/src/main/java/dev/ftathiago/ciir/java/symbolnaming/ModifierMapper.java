package dev.ftathiago.ciir.java.symbolnaming;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import dev.ftathiago.ciir.core.symbols.CiirModifier;
import dev.ftathiago.ciir.core.symbols.CiirModifierOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads modifier keywords straight off a declaration's own syntax (not derived boolean symbol
 * flags), maps the ones CIIR preserves, and sorts them into {@link CiirModifierOrder#CANONICAL}
 * order regardless of declaration order in source.
 */
public final class ModifierMapper {

  private ModifierMapper() {}

  public static List<CiirModifier> map(NodeList<Modifier> modifiers) {
    var mapped = new ArrayList<CiirModifier>();
    for (var modifier : modifiers) {
      toCiirModifier(modifier.getKeyword()).ifPresent(mapped::add);
    }
    return CiirModifierOrder.sort(mapped);
  }

  private static java.util.Optional<CiirModifier> toCiirModifier(Modifier.Keyword keyword) {
    return java.util.Optional.ofNullable(
        switch (keyword) {
          case STATIC -> CiirModifier.STATIC;
          case FINAL -> CiirModifier.FINAL;
          case TRANSIENT -> CiirModifier.TRANSIENT;
          case VOLATILE -> CiirModifier.VOLATILE;
          case NATIVE -> CiirModifier.NATIVE;
          case SYNCHRONIZED -> CiirModifier.SYNCHRONIZED;
          case ABSTRACT -> CiirModifier.ABSTRACT;
          case SEALED -> CiirModifier.SEALED;
          case NON_SEALED -> CiirModifier.NON_SEALED;
          case DEFAULT -> CiirModifier.DEFAULT;
          case STRICTFP -> CiirModifier.STRICTFP;
          // Accessibility keywords (public/protected/private) are handled by
          // AccessibilityMapper;
          // TRANSITIVE is a module-info-only keyword, not applicable to type/member
          // declarations.
          default -> null;
        });
  }
}
