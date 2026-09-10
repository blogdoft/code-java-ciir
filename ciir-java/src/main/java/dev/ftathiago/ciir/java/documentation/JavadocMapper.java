package dev.ftathiago.ciir.java.documentation;

import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import dev.ftathiago.ciir.core.documentation.CiirDocumentation;
import dev.ftathiago.ciir.core.documentation.CiirDocumentationFormat;
import dev.ftathiago.ciir.core.documentation.CiirDocumentationParameter;
import dev.ftathiago.ciir.core.documentation.CiirDocumentationSource;
import dev.ftathiago.ciir.core.documentation.CiirExceptionDocumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Maps a JavaParser {@link Javadoc} block into {@link CiirDocumentation}. */
public final class JavadocMapper {

  private JavadocMapper() {}

  public static Optional<CiirDocumentation> map(Optional<Javadoc> javadoc) {
    return javadoc.map(JavadocMapper::mapPresent);
  }

  private static CiirDocumentation mapPresent(Javadoc javadoc) {
    var parameters = new ArrayList<CiirDocumentationParameter>();
    var exceptions = new ArrayList<CiirExceptionDocumentation>();
    String returns = null;

    for (var tag : javadoc.getBlockTags()) {
      if (tag.getType() == JavadocBlockTag.Type.PARAM) {
        tag.getName()
            .ifPresent(
                name ->
                    parameters.add(
                        new CiirDocumentationParameter(name, tag.getContent().toText())));
      } else if (tag.getType() == JavadocBlockTag.Type.RETURN) {
        returns = tag.getContent().toText();
      } else if (tag.getType() == JavadocBlockTag.Type.THROWS
          || tag.getType() == JavadocBlockTag.Type.EXCEPTION) {
        exceptions.add(
            new CiirExceptionDocumentation(
                tag.getName().orElse("unknown"), tag.getContent().toText()));
      }
    }

    var summary = javadoc.getDescription().toText();
    return new CiirDocumentation(
        CiirDocumentationFormat.JAVADOC,
        CiirDocumentationSource.DECLARED,
        summary.isBlank() ? null : summary,
        null,
        List.copyOf(parameters),
        returns,
        List.copyOf(exceptions));
  }
}
