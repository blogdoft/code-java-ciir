package dev.ftathiago.ciir.java.comments;

import com.github.javaparser.ast.comments.Comment;
import dev.ftathiago.ciir.core.comments.CiirComment;
import dev.ftathiago.ciir.core.comments.CiirCommentKind;
import dev.ftathiago.ciir.java.source.SourceLocationFactory;
import java.util.Locale;
import java.util.regex.Pattern;

/** Classifies an ordinary (non-Javadoc) comment by its explicit marker, else by comment syntax. */
public final class CommentClassifier {

  private static final Pattern MARKER =
      Pattern.compile("^\\s*(TODO|FIXME|WARNING|NOTE)\\b", Pattern.CASE_INSENSITIVE);

  private CommentClassifier() {}

  public static CiirComment classify(Comment comment) {
    var text = comment.getContent().strip();
    return new CiirComment(kindOf(comment, text), text, SourceLocationFactory.rangeOf(comment));
  }

  private static CiirCommentKind kindOf(Comment comment, String text) {
    var matcher = MARKER.matcher(text);
    if (matcher.find()) {
      return switch (matcher.group(1).toUpperCase(Locale.ROOT)) {
        case "TODO" -> CiirCommentKind.TODO;
        case "FIXME" -> CiirCommentKind.FIXME;
        case "WARNING" -> CiirCommentKind.WARNING;
        default -> CiirCommentKind.NOTE;
      };
    }
    return comment.isLineComment() ? CiirCommentKind.LINE : CiirCommentKind.BLOCK;
  }
}
