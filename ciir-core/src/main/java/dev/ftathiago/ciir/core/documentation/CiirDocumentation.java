package dev.ftathiago.ciir.core.documentation;

import java.util.List;
import java.util.Objects;

/** Formal doc-comment extraction result (Javadoc, for this generator). */
public record CiirDocumentation(
    CiirDocumentationFormat format,
    CiirDocumentationSource source,
    String summary,
    String remarks,
    List<CiirDocumentationParameter> parameters,
    String returns,
    List<CiirExceptionDocumentation> exceptions) {

  public CiirDocumentation {
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(source, "source");
    parameters = parameters == null ? List.of() : List.copyOf(parameters);
    exceptions = exceptions == null ? List.of() : List.copyOf(exceptions);
  }

  public static CiirDocumentation declaredJavadoc() {
    return new CiirDocumentation(
        CiirDocumentationFormat.JAVADOC,
        CiirDocumentationSource.DECLARED,
        null,
        null,
        List.of(),
        null,
        List.of());
  }
}
