package dev.ftathiago.ciir.core.extensions;

import java.util.Map;

/**
 * Language-specific escape hatch for details that don't (yet) belong in the universal CIIR model.
 * The core model never grows first-class fields for single-language concepts; they live under
 * {@code extensions.java} instead.
 */
public record CiirExtensions(Map<String, Object> java) {

  public CiirExtensions {
    java = java == null ? Map.of() : Map.copyOf(java);
  }

  public static CiirExtensions empty() {
    return new CiirExtensions(Map.of());
  }
}
