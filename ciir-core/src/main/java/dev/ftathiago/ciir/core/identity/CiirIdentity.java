package dev.ftathiago.ciir.core.identity;

import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.CiirKindNames;
import dev.ftathiago.ciir.core.hashing.Sha256Text;

/**
 * Deterministic CIIR document identity. Two analyses of the same semantic symbol MUST produce the
 * same {@code id}; overloads MUST produce different ids because {@code canonicalSymbolIdentity}
 * includes parameter types for methods/constructors.
 */
public final class CiirIdentity {

  private static final char SEPARATOR = '|';

  private CiirIdentity() {}

  /**
   * Joins the four identity components with a literal {@code |}. None of these components are ever
   * expected to contain {@code |} themselves, so no escaping is performed.
   */
  public static String buildCanonicalKey(
      String language, String projectIdentity, CiirKind kind, String canonicalSymbolIdentity) {
    return String.join(
        String.valueOf(SEPARATOR),
        language,
        projectIdentity,
        CiirKindNames.toToken(kind),
        canonicalSymbolIdentity);
  }

  /** {@code sha256:<hex>} of {@link #buildCanonicalKey}'s result. */
  public static String computeId(
      String language, String projectIdentity, CiirKind kind, String canonicalSymbolIdentity) {
    return Sha256Text.computePrefixedHash(
        buildCanonicalKey(language, projectIdentity, kind, canonicalSymbolIdentity));
  }
}
