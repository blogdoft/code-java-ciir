package dev.ftathiago.ciir.core.embeddingtext;

import dev.ftathiago.ciir.core.comments.CiirComment;
import dev.ftathiago.ciir.core.conditions.CiirCondition;
import dev.ftathiago.ciir.core.relations.CiirRelation;

/**
 * The single, testable seam deciding which relations/conditions/comments are relevant enough to
 * surface in {@code embeddingText} — kept out of the analyzer, never scattered {@code if}s.
 */
public interface EmbeddingTextPolicy {

  boolean shouldIncludeRelation(CiirRelation relation);

  boolean shouldIncludeCondition(CiirCondition condition);

  boolean shouldIncludeComment(CiirComment comment);
}
