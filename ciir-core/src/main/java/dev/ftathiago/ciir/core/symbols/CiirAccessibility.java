package dev.ftathiago.ciir.core.symbols;

/**
 * A declaration's access level. Java has no analogue to C#'s {@code internal} / {@code protected
 * internal} / {@code private protected} combinations, so this is a Java-specific enum rather than a
 * reuse of another language's token set.
 */
public enum CiirAccessibility {
  UNKNOWN,
  PRIVATE,
  PACKAGE_PRIVATE,
  PROTECTED,
  PUBLIC
}
