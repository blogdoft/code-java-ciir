package dev.ftathiago.ciir.core.source;

import java.util.Objects;

/**
 * An entity's primary declaration location.
 *
 * @param path relative to the analysis root — never an absolute machine path.
 * @param hash {@code sha256:<hex>} of the exact source span bytes; {@code null} when not computed.
 * @param text the literal source text of the span; only populated with {@code --include-source}.
 */
public record CiirSourceLocation(
    String path,
    int startLine,
    int startColumn,
    int endLine,
    int endColumn,
    String hash,
    String text) {

  public CiirSourceLocation {
    Objects.requireNonNull(path, "path");
  }

  public static CiirSourceLocation of(
      String path, int startLine, int startColumn, int endLine, int endColumn) {
    return new CiirSourceLocation(path, startLine, startColumn, endLine, endColumn, null, null);
  }

  public CiirSourceLocation withHash(String hash) {
    return new CiirSourceLocation(path, startLine, startColumn, endLine, endColumn, hash, text);
  }

  public CiirSourceLocation withText(String text) {
    return new CiirSourceLocation(path, startLine, startColumn, endLine, endColumn, hash, text);
  }
}
