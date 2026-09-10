package dev.ftathiago.ciir.java.symbolnaming;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import dev.ftathiago.ciir.core.symbols.CiirAccessibility;

public final class AccessibilityMapper {

  private AccessibilityMapper() {}

  /**
   * Derived directly from the declaration's raw modifier keywords (not a shared {@code
   * getAccessSpecifier()} accessor — JavaParser's concrete AST declaration classes expose {@code
   * getModifiers()} without uniformly implementing the {@code NodeWithModifiers}/{@code
   * NodeWithAccessModifiers} default-method interfaces). No public/protected/private keyword
   * present means Java's default package-private visibility.
   */
  public static CiirAccessibility fromModifiers(NodeList<Modifier> modifiers) {
    for (var modifier : modifiers) {
      switch (modifier.getKeyword()) {
        case PUBLIC -> {
          return CiirAccessibility.PUBLIC;
        }
        case PROTECTED -> {
          return CiirAccessibility.PROTECTED;
        }
        case PRIVATE -> {
          return CiirAccessibility.PRIVATE;
        }
        default -> {
          // not an accessibility keyword; keep scanning
        }
      }
    }
    return CiirAccessibility.PACKAGE_PRIVATE;
  }
}
