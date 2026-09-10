package dev.ftathiago.ciir.core.symbols;

import java.util.Objects;

/**
 * A CIIR document's identity block.
 *
 * @param name simple name.
 * @param qualifiedName fully qualified, human-readable name.
 * @param canonicalName the unambiguous identity of the symbol; for methods/constructors this
 *     includes the parameter type list so overloads are distinguishable. Feeds the document's
 *     {@code id} hash.
 * @param container the qualified name of the semantically owning entity (containing type or
 *     package), or {@code null} when there is none (top-level package/project).
 */
public record CiirSymbol(
    String name, String qualifiedName, String canonicalName, String container) {

  public CiirSymbol {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(qualifiedName, "qualifiedName");
    Objects.requireNonNull(canonicalName, "canonicalName");
  }

  public static CiirSymbol of(String name, String qualifiedName, String canonicalName) {
    return new CiirSymbol(name, qualifiedName, canonicalName, null);
  }
}
