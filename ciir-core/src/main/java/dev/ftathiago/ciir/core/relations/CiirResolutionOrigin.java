package dev.ftathiago.ciir.core.relations;

/**
 * Where a relation's target symbol was found. {@code PROJECT} = the analyzed Maven module itself;
 * {@code SOLUTION} = a different Maven module analyzed in this same run; {@code FRAMEWORK} = the
 * JDK; {@code DEPENDENCY} = a resolved third-party Maven artifact.
 */
public enum CiirResolutionOrigin {
  UNKNOWN,
  PROJECT,
  SOLUTION,
  DEPENDENCY,
  FRAMEWORK,
  RUNTIME,
  EXTERNAL_SERVICE
}
