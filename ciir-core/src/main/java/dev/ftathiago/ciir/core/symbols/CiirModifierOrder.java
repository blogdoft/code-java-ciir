package dev.ftathiago.ciir.core.symbols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Canonical, deterministic ordering for {@link CiirModifier} lists, independent of the order the
 * modifiers appeared in source. {@link #sort(Collection)} projects the canonical order down to
 * whatever modifiers are actually present, dropping duplicates.
 */
public final class CiirModifierOrder {

  /** The fixed canonical order every modifiers[] array is emitted in. */
  public static final List<CiirModifier> CANONICAL =
      List.of(
          CiirModifier.STATIC,
          CiirModifier.FINAL,
          CiirModifier.TRANSIENT,
          CiirModifier.VOLATILE,
          CiirModifier.NATIVE,
          CiirModifier.SYNCHRONIZED,
          CiirModifier.ABSTRACT,
          CiirModifier.SEALED,
          CiirModifier.NON_SEALED,
          CiirModifier.DEFAULT,
          CiirModifier.STRICTFP);

  private CiirModifierOrder() {}

  /** Returns {@code modifiers}, deduplicated and reordered into {@link #CANONICAL} order. */
  public static List<CiirModifier> sort(Collection<CiirModifier> modifiers) {
    var present = EnumSet.noneOf(CiirModifier.class);
    present.addAll(modifiers);

    var result = new ArrayList<CiirModifier>(present.size());
    for (var modifier : CANONICAL) {
      if (present.contains(modifier)) {
        result.add(modifier);
      }
    }
    return List.copyOf(result);
  }
}
