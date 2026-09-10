package dev.ftathiago.ciir.core.relations;

/**
 * The kind of statically observable fact a {@link CiirRelation} records. {@code CONTAINS} is never
 * emitted by the Java generator: containment is already recoverable from every child document's
 * {@code symbol.container}.
 */
public enum CiirRelationKind {
  CONTAINS,
  INHERITS,
  IMPLEMENTS,
  OVERRIDES,
  CALLS,
  CONSTRUCTS,
  READS,
  WRITES,
  THROWS,
  CATCHES
}
