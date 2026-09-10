package dev.ftathiago.ciir.core.symbols;

import java.util.Objects;

/**
 * A method/constructor parameter, in declaration order. {@code type} is the fully qualified
 * parameter type.
 */
public record CiirParameter(String name, String type) {

  public CiirParameter {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
  }
}
