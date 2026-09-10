package dev.ftathiago.ciir.core.source;

/**
 * A 1-based line/column range within the same file as the owning entity's source location. Used for
 * nested locations (comments/relations/conditions) where repeating the file path/hash would be
 * redundant. Unlike {@link CiirSourceLocation}, columns are optional here.
 */
public record CiirRange(int startLine, Integer startColumn, int endLine, Integer endColumn) {

  public static CiirRange of(int startLine, int endLine) {
    return new CiirRange(startLine, null, endLine, null);
  }

  public static CiirRange of(int startLine, int startColumn, int endLine, int endColumn) {
    return new CiirRange(startLine, startColumn, endLine, endColumn);
  }
}
