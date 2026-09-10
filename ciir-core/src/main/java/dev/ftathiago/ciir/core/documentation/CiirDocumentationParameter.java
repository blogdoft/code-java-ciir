package dev.ftathiago.ciir.core.documentation;

import java.util.Objects;

/** One documented {@code @param}. */
public record CiirDocumentationParameter(String name, String description) {

  public CiirDocumentationParameter {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
  }
}
