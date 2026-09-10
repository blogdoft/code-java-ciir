package dev.ftathiago.ciir.core;

/**
 * The kind of software entity a {@link CiirDocument} represents. The Java v1 generator only
 * produces these six values; the CIIR JSON Schema reserves additional kinds ({@code property},
 * {@code event}, {@code database}, ...) for other language/domain generators — this enum models
 * only what this generator actually emits (YAGNI).
 */
public enum CiirKind {
  PROJECT,
  NAMESPACE,
  TYPE,
  METHOD,
  CONSTRUCTOR,
  FIELD
}
