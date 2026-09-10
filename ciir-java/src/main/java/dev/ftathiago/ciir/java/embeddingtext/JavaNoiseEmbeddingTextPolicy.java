package dev.ftathiago.ciir.java.embeddingtext;

import dev.ftathiago.ciir.core.comments.CiirComment;
import dev.ftathiago.ciir.core.comments.CiirCommentKind;
import dev.ftathiago.ciir.core.conditions.CiirCondition;
import dev.ftathiago.ciir.core.embeddingtext.EmbeddingTextPolicy;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.relations.CiirRelationKind;
import java.util.List;
import java.util.Set;

/**
 * The Java-specific noise filter for {@code embeddingText}: only {@code calls} relations are ever
 * filtered (by a fully qualified symbol prefix match), conditions are never filtered, and only
 * marker comments ({@code TODO}/{@code FIXME}/{@code WARNING}/{@code NOTE}) reach the embedding
 * projection — plain comments stay in {@code comments[]} but never in {@code embeddingText}. This
 * list is a fresh judgment call for Java (JDK/common-library equivalents of the C# generator's BCL
 * noise list), not extracted from the C# implementation.
 */
public final class JavaNoiseEmbeddingTextPolicy implements EmbeddingTextPolicy {

  private static final List<String> NOISY_CALL_PREFIXES =
      List.of(
          "java.lang.String.isEmpty",
          "java.lang.String.isBlank",
          "java.lang.String.valueOf",
          "java.lang.Object.toString",
          "java.lang.Object.equals",
          "java.lang.Object.hashCode",
          "java.util.Objects.requireNonNull",
          "java.util.Objects.equals",
          "java.util.Objects.hash",
          "java.util.List.of",
          "java.util.Map.of",
          "java.util.Set.of",
          "java.util.stream.Stream.of",
          "java.util.stream.Collectors.",
          "java.lang.System.out",
          "java.lang.System.err",
          "java.io.PrintStream.println",
          "java.io.PrintStream.print",
          "org.slf4j.Logger.",
          "java.util.logging.Logger.",
          "java.util.Optional.");

  private static final Set<CiirCommentKind> EMBEDDED_COMMENT_KINDS =
      Set.of(
          CiirCommentKind.TODO,
          CiirCommentKind.FIXME,
          CiirCommentKind.WARNING,
          CiirCommentKind.NOTE);

  @Override
  public boolean shouldIncludeRelation(CiirRelation relation) {
    if (relation.kind() != CiirRelationKind.CALLS) {
      return true;
    }
    var symbol = relation.target().symbol();
    return NOISY_CALL_PREFIXES.stream().noneMatch(symbol::startsWith);
  }

  @Override
  public boolean shouldIncludeCondition(CiirCondition condition) {
    return true;
  }

  @Override
  public boolean shouldIncludeComment(CiirComment comment) {
    return EMBEDDED_COMMENT_KINDS.contains(comment.kind());
  }
}
