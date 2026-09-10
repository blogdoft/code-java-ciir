package dev.ftathiago.ciir.core;

import java.util.Locale;

/**
 * Canonical lowercase string token per {@link CiirKind}, shared by {@code CiirIdentity} (the id
 * hash) and {@code EmbeddingTextBuilder} (the "Entity: " line) — the two must always agree on
 * spelling.
 */
public final class CiirKindNames {

  private CiirKindNames() {}

  public static String toToken(CiirKind kind) {
    return kind.name().toLowerCase(Locale.ROOT);
  }
}
