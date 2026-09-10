package dev.ftathiago.ciir.core.relations;

/** How confidently a relation's target symbol could be resolved statically. */
public enum CiirResolutionStatus {
  RESOLVED,
  UNRESOLVED,
  AMBIGUOUS,
  EXTERNAL,
  DYNAMIC
}
