package dev.ftathiago.ciir.core.symbols;

import java.util.List;
import java.util.Objects;

/** The {@code type}-kind detail block. */
public record CiirTypeInfo(
    CiirTypeKind typeKind,
    CiirAccessibility accessibility,
    List<CiirModifier> modifiers,
    List<String> genericParameters) {

  public CiirTypeInfo {
    Objects.requireNonNull(typeKind, "typeKind");
    Objects.requireNonNull(accessibility, "accessibility");
    modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    genericParameters = genericParameters == null ? List.of() : List.copyOf(genericParameters);
  }

  public static CiirTypeInfo of(CiirTypeKind typeKind, CiirAccessibility accessibility) {
    return new CiirTypeInfo(typeKind, accessibility, List.of(), List.of());
  }
}
