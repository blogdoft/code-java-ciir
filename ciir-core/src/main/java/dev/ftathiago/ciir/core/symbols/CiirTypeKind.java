package dev.ftathiago.ciir.core.symbols;

/**
 * The kind of Java type declaration. {@code RECORD} covers Java 16+ records; {@code ANNOTATION}
 * covers {@code @interface} declarations (no C# analogue). Java has no {@code struct}/ {@code
 * delegate} equivalents.
 */
public enum CiirTypeKind {
  CLASS,
  INTERFACE,
  ENUM,
  RECORD,
  ANNOTATION,
  UNKNOWN
}
