package dev.ftathiago.ciir.core.relations;

import java.util.Objects;

/**
 * A relation's target.
 *
 * @param id the target's own CIIR document id, populated only when {@code resolution.status} is
 *     {@code RESOLVED} (origin {@code PROJECT} or {@code SOLUTION}) and the target's kind is one
 *     this generator emits its own document for. {@code null} otherwise — never fabricated.
 * @param symbol the fully qualified target symbol string.
 */
public record CiirRelationTarget(String id, String symbol) {

  public CiirRelationTarget {
    Objects.requireNonNull(symbol, "symbol");
  }

  public static CiirRelationTarget of(String symbol) {
    return new CiirRelationTarget(null, symbol);
  }
}
