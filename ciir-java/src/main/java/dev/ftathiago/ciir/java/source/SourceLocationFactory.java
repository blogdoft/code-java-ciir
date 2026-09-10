package dev.ftathiago.ciir.java.source;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import dev.ftathiago.ciir.core.hashing.Sha256Text;
import dev.ftathiago.ciir.core.source.CiirRange;
import dev.ftathiago.ciir.core.source.CiirSourceLocation;
import java.util.List;

/**
 * Builds {@link CiirSourceLocation}/{@link CiirRange} from a JavaParser {@link Node}'s {@link
 * Range} (1-based line/column, inclusive on both ends), extracting the literal source span text
 * (never a pretty-printed reconstruction) for hashing and, when requested, embedding.
 */
public final class SourceLocationFactory {

  private SourceLocationFactory() {}

  public static CiirSourceLocation create(
      Node node, String relativePath, List<String> sourceLines, boolean includeSource) {
    var range = requireRange(node);
    var span = extractSpan(sourceLines, range);
    var hash = Sha256Text.computePrefixedHash(span);
    return new CiirSourceLocation(
        relativePath,
        range.begin.line,
        range.begin.column,
        range.end.line,
        range.end.column,
        hash,
        includeSource ? span : null);
  }

  public static CiirRange rangeOf(Node node) {
    var range = requireRange(node);
    return CiirRange.of(range.begin.line, range.begin.column, range.end.line, range.end.column);
  }

  private static Range requireRange(Node node) {
    return node.getRange()
        .orElseThrow(() -> new IllegalStateException("Node has no source range: " + node));
  }

  private static String extractSpan(List<String> lines, Range range) {
    if (range.begin.line == range.end.line) {
      var line = lines.get(range.begin.line - 1);
      return line.substring(range.begin.column - 1, Math.min(range.end.column, line.length()));
    }

    var builder = new StringBuilder();
    var firstLine = lines.get(range.begin.line - 1);
    builder.append(firstLine.substring(Math.min(range.begin.column - 1, firstLine.length())));

    for (int lineNumber = range.begin.line + 1; lineNumber < range.end.line; lineNumber++) {
      builder.append('\n').append(lines.get(lineNumber - 1));
    }

    var lastLine = lines.get(range.end.line - 1);
    builder.append('\n').append(lastLine, 0, Math.min(range.end.column, lastLine.length()));
    return builder.toString();
  }
}
