package dev.ftathiago.ciir.core.comments;

import dev.ftathiago.ciir.core.source.CiirRange;
import java.util.Objects;

/** One preserved ordinary source comment. {@code text} has comment markers already stripped. */
public record CiirComment(CiirCommentKind kind, String text, CiirRange location) {

  public CiirComment {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(location, "location");
  }
}
