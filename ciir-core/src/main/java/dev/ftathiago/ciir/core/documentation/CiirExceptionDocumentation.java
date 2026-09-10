package dev.ftathiago.ciir.core.documentation;

import java.util.Objects;

/** One documented {@code @throws}/{@code @exception}. */
public record CiirExceptionDocumentation(String type, String description) {

  public CiirExceptionDocumentation {
    Objects.requireNonNull(type, "type");
  }
}
