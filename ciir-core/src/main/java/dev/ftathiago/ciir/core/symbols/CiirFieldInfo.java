package dev.ftathiago.ciir.core.symbols;

import java.util.List;
import java.util.Objects;

/** The {@code field}-kind detail block. */
public record CiirFieldInfo(
    CiirAccessibility accessibility, List<CiirModifier> modifiers, String type) {

  public CiirFieldInfo {
    Objects.requireNonNull(accessibility, "accessibility");
    Objects.requireNonNull(type, "type");
    modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
  }

  public static CiirFieldInfo of(CiirAccessibility accessibility, String type) {
    return new CiirFieldInfo(accessibility, List.of(), type);
  }
}
