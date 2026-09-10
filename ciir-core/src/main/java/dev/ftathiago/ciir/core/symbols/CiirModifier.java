package dev.ftathiago.ciir.core.symbols;

/**
 * A Java declaration modifier CIIR preserves. This is a Java-specific token set (distinct from the
 * C# generator's modifier vocabulary): {@code sealed}/{@code non-sealed} are Java 17+ class
 * modifiers with no C# analogue, and Java has no {@code const}/{@code readonly}/{@code extern}/
 * {@code virtual}/{@code override}/{@code unsafe}/{@code partial}/{@code required}/{@code async}
 * keywords.
 */
public enum CiirModifier {
  STATIC,
  FINAL,
  TRANSIENT,
  VOLATILE,
  NATIVE,
  SYNCHRONIZED,
  ABSTRACT,
  SEALED,
  NON_SEALED,
  DEFAULT,
  STRICTFP
}
