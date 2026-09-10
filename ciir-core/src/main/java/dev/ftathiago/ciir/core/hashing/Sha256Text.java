package dev.ftathiago.ciir.core.hashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Shared SHA-256 hashing utility, producing the CIIR contract's {@code sha256:<hex>} format. */
public final class Sha256Text {

  private static final String PREFIX = "sha256:";

  private Sha256Text() {}

  /** Lowercase hex SHA-256 digest of {@code text}, UTF-8 encoded, with no prefix. */
  public static String computeHash(String text) {
    return computeHash(text.getBytes(StandardCharsets.UTF_8));
  }

  /** Lowercase hex SHA-256 digest of {@code bytes}, with no prefix. */
  public static String computeHash(byte[] bytes) {
    return HexFormat.of().formatHex(digest(bytes));
  }

  /** {@code sha256:<hex>} of {@code text}, UTF-8 encoded. */
  public static String computePrefixedHash(String text) {
    return PREFIX + computeHash(text);
  }

  /** {@code sha256:<hex>} of {@code bytes}. */
  public static String computePrefixedHash(byte[] bytes) {
    return PREFIX + computeHash(bytes);
  }

  private static byte[] digest(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available on this JVM", e);
    }
  }
}
