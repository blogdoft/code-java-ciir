package dev.ftathiago.ciir.core.conditions;

import dev.ftathiago.ciir.core.source.CiirRange;
import java.util.List;
import java.util.Objects;

/**
 * A preserved branching/looping construct, as a verbatim fact — never interpreted as a business
 * rule.
 *
 * @param expression the literal source text of the condition.
 * @param reads fully-qualified symbols resolved within the expression.
 */
public record CiirCondition(
    CiirConditionKind kind, String expression, CiirRange location, List<String> reads) {

  public CiirCondition {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(location, "location");
    reads = reads == null ? List.of() : List.copyOf(reads);
  }

  public static CiirCondition of(CiirConditionKind kind, String expression, CiirRange location) {
    return new CiirCondition(kind, expression, location, List.of());
  }
}
