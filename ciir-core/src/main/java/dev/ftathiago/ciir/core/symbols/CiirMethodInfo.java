package dev.ftathiago.ciir.core.symbols;

import java.util.List;
import java.util.Objects;

/**
 * The {@code method}/{@code constructor}-kind detail block.
 *
 * @param returnType {@code null} for constructors, which have no return type.
 * @param embeddingReturnType the semantic return type used for the embeddingText "Returns" section
 *     (e.g. an unwrapped {@code Optional<T>}); falls back to {@code returnType} when {@code null}.
 */
public record CiirMethodInfo(
    CiirAccessibility accessibility,
    List<CiirModifier> modifiers,
    List<CiirParameter> parameters,
    String returnType,
    String embeddingReturnType) {

  public CiirMethodInfo {
    Objects.requireNonNull(accessibility, "accessibility");
    modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    parameters = parameters == null ? List.of() : List.copyOf(parameters);
  }

  public static CiirMethodInfo of(
      CiirAccessibility accessibility,
      List<CiirModifier> modifiers,
      List<CiirParameter> parameters,
      String returnType) {
    return new CiirMethodInfo(accessibility, modifiers, parameters, returnType, null);
  }
}
