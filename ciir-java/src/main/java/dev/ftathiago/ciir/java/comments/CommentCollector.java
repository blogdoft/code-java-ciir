package dev.ftathiago.ciir.java.comments;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import dev.ftathiago.ciir.core.comments.CiirComment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Gathers a declaration's own leading comment plus every comment physically inside its range. */
public final class CommentCollector {

  private CommentCollector() {}

  public static List<CiirComment> collect(Node node) {
    Set<Comment> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    var comments = new ArrayList<CiirComment>();

    node.getComment().filter(c -> !c.isJavadocComment()).ifPresent(c -> addOnce(c, seen, comments));
    for (var comment : node.getAllContainedComments()) {
      if (!comment.isJavadocComment()) {
        addOnce(comment, seen, comments);
      }
    }
    return comments;
  }

  private static void addOnce(Comment comment, Set<Comment> seen, List<CiirComment> comments) {
    if (seen.add(comment)) {
      comments.add(CommentClassifier.classify(comment));
    }
  }
}
