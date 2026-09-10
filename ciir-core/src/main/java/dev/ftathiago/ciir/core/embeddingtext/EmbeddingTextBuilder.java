package dev.ftathiago.ciir.core.embeddingtext;

import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.core.CiirKindNames;
import dev.ftathiago.ciir.core.comments.CiirComment;
import dev.ftathiago.ciir.core.conditions.CiirCondition;
import dev.ftathiago.ciir.core.documentation.CiirDocumentation;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.relations.CiirRelationKind;
import dev.ftathiago.ciir.core.symbols.CiirMethodInfo;
import dev.ftathiago.ciir.core.symbols.CiirParameter;
import java.util.ArrayList;
import java.util.List;

/**
 * The deterministic {@code semantic-v1} text-projection algorithm. {@code embeddingText} is a
 * semantic projection, not a copy of the full document: which relations/comments/conditions are
 * relevant enough to include is decided entirely by the supplied {@link EmbeddingTextPolicy}.
 *
 * <p>Section order (each omitted entirely, header included, when it has no content): Entity,
 * Qualified name, Container, Documentation, Parameters, Returns, Comments, Reads, Writes, Calls,
 * Constructs, Throws, Conditions. Lines are joined with {@code \n}, no trailing newline.
 */
public final class EmbeddingTextBuilder {

  /** The name of this strategy, as recorded in {@code CiirDocument.embeddingTextStrategy}. */
  public static final String STRATEGY_NAME = "semantic-v1";

  private EmbeddingTextBuilder() {}

  public static String build(CiirDocument document, EmbeddingTextPolicy policy) {
    var lines = new ArrayList<String>();

    lines.add("Entity: " + CiirKindNames.toToken(document.kind()));
    lines.add("Qualified name: " + document.symbol().qualifiedName());

    var container = document.symbol().container();
    if (container != null && !container.isEmpty()) {
      lines.add("Container: " + container);
    }

    appendDocumentation(lines, document.documentation());
    appendParameters(lines, document.method());
    appendReturns(lines, document.method());
    appendComments(lines, document.comments(), policy);
    appendRelations(lines, document.relations(), CiirRelationKind.READS, "Reads", policy);
    appendRelations(lines, document.relations(), CiirRelationKind.WRITES, "Writes", policy);
    appendRelations(lines, document.relations(), CiirRelationKind.CALLS, "Calls", policy);
    appendRelations(lines, document.relations(), CiirRelationKind.CONSTRUCTS, "Constructs", policy);
    appendRelations(lines, document.relations(), CiirRelationKind.THROWS, "Throws", policy);
    appendConditions(lines, document.conditions(), policy);

    return String.join("\n", lines);
  }

  private static void appendDocumentation(List<String> lines, CiirDocumentation documentation) {
    if (documentation == null) {
      return;
    }
    if (documentation.summary() != null && !documentation.summary().isEmpty()) {
      lines.add("Documentation: " + documentation.summary());
    }
    if (documentation.remarks() != null && !documentation.remarks().isEmpty()) {
      lines.add("Remarks: " + documentation.remarks());
    }
  }

  private static void appendParameters(List<String> lines, CiirMethodInfo method) {
    if (method == null || method.parameters().isEmpty()) {
      return;
    }
    lines.add("Parameters:");
    for (CiirParameter parameter : method.parameters()) {
      lines.add("- " + parameter.name() + ": " + parameter.type());
    }
  }

  private static void appendReturns(List<String> lines, CiirMethodInfo method) {
    if (method == null) {
      return;
    }
    var returnType =
        method.embeddingReturnType() != null ? method.embeddingReturnType() : method.returnType();
    if (returnType != null && !returnType.isEmpty()) {
      lines.add("Returns: " + returnType);
    }
  }

  private static void appendComments(
      List<String> lines, List<CiirComment> comments, EmbeddingTextPolicy policy) {
    var relevant = comments.stream().filter(policy::shouldIncludeComment).toList();
    if (relevant.isEmpty()) {
      return;
    }
    lines.add("Comments:");
    for (CiirComment comment : relevant) {
      lines.add("- " + comment.text());
    }
  }

  private static void appendRelations(
      List<String> lines,
      List<CiirRelation> relations,
      CiirRelationKind kind,
      String label,
      EmbeddingTextPolicy policy) {
    var relevant =
        relations.stream()
            .filter(relation -> relation.kind() == kind)
            .filter(policy::shouldIncludeRelation)
            .toList();
    if (relevant.isEmpty()) {
      return;
    }
    lines.add(label + ":");
    for (CiirRelation relation : relevant) {
      lines.add("- " + relation.target().symbol());
    }
  }

  private static void appendConditions(
      List<String> lines, List<CiirCondition> conditions, EmbeddingTextPolicy policy) {
    var relevant = conditions.stream().filter(policy::shouldIncludeCondition).toList();
    if (relevant.isEmpty()) {
      return;
    }
    lines.add("Conditions:");
    for (CiirCondition condition : relevant) {
      lines.add("- " + condition.expression());
    }
  }
}
