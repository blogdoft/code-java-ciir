package dev.ftathiago.ciir.core.embeddingtext;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.comments.CiirComment;
import dev.ftathiago.ciir.core.conditions.CiirCondition;
import dev.ftathiago.ciir.core.documentation.CiirDocumentation;
import dev.ftathiago.ciir.core.documentation.CiirDocumentationFormat;
import dev.ftathiago.ciir.core.documentation.CiirDocumentationSource;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.relations.CiirRelationKind;
import dev.ftathiago.ciir.core.relations.CiirRelationResolution;
import dev.ftathiago.ciir.core.relations.CiirRelationTarget;
import dev.ftathiago.ciir.core.relations.CiirResolutionOrigin;
import dev.ftathiago.ciir.core.relations.CiirResolutionStatus;
import dev.ftathiago.ciir.core.symbols.CiirAccessibility;
import dev.ftathiago.ciir.core.symbols.CiirMethodInfo;
import dev.ftathiago.ciir.core.symbols.CiirParameter;
import dev.ftathiago.ciir.core.symbols.CiirSymbol;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingTextBuilderTest {

  private static final EmbeddingTextPolicy ALLOW_ALL =
      new EmbeddingTextPolicy() {
        @Override
        public boolean shouldIncludeRelation(CiirRelation relation) {
          return true;
        }

        @Override
        public boolean shouldIncludeCondition(CiirCondition condition) {
          return true;
        }

        @Override
        public boolean shouldIncludeComment(CiirComment comment) {
          return true;
        }
      };

  private static final EmbeddingTextPolicy DENY_ALL =
      new EmbeddingTextPolicy() {
        @Override
        public boolean shouldIncludeRelation(CiirRelation relation) {
          return false;
        }

        @Override
        public boolean shouldIncludeCondition(CiirCondition condition) {
          return false;
        }

        @Override
        public boolean shouldIncludeComment(CiirComment comment) {
          return false;
        }
      };

  @Test
  void build_matchesWorkedExample() {
    var document =
        CiirDocument.builder()
            .id("sha256:doesnotmatterforthistest")
            .kind(CiirKind.METHOD)
            .language("java")
            .project("payments-application")
            .symbol(
                new CiirSymbol(
                    "authorize",
                    "dev.ftathiago.payments.application.PaymentService.authorize",
                    "dev.ftathiago.payments.application.PaymentService.authorize(dev.ftathiago.payments.domain.Order)",
                    "dev.ftathiago.payments.application.PaymentService"))
            .documentation(
                new CiirDocumentation(
                    CiirDocumentationFormat.JAVADOC,
                    CiirDocumentationSource.DECLARED,
                    "Authorizes a payment for the given order.",
                    null,
                    List.of(),
                    null,
                    List.of()))
            .method(
                CiirMethodInfo.of(
                    CiirAccessibility.PUBLIC,
                    List.of(),
                    List.of(new CiirParameter("order", "dev.ftathiago.payments.domain.Order")),
                    "dev.ftathiago.payments.domain.PaymentResult"))
            .addRelation(reads("dev.ftathiago.payments.domain.Order.total"))
            .addRelation(calls("dev.ftathiago.payments.domain.PaymentGateway.authorize"))
            .addRelation(throwsRelation("dev.ftathiago.payments.domain.InvalidOrderException"))
            .build();

    var text = EmbeddingTextBuilder.build(document, ALLOW_ALL);

    assertThat(text)
        .isEqualTo(
            String.join(
                "\n",
                "Entity: method",
                "Qualified name: dev.ftathiago.payments.application.PaymentService.authorize",
                "Container: dev.ftathiago.payments.application.PaymentService",
                "Documentation: Authorizes a payment for the given order.",
                "Parameters:",
                "- order: dev.ftathiago.payments.domain.Order",
                "Returns: dev.ftathiago.payments.domain.PaymentResult",
                "Reads:",
                "- dev.ftathiago.payments.domain.Order.total",
                "Calls:",
                "- dev.ftathiago.payments.domain.PaymentGateway.authorize",
                "Throws:",
                "- dev.ftathiago.payments.domain.InvalidOrderException"));
  }

  @Test
  void build_bareTypeOmitsEveryOptionalSection() {
    var document =
        CiirDocument.builder()
            .id("sha256:doesnotmatterforthistest")
            .kind(CiirKind.TYPE)
            .language("java")
            .project("payments-domain")
            .symbol(
                CiirSymbol.of(
                    "Order",
                    "dev.ftathiago.payments.domain.Order",
                    "dev.ftathiago.payments.domain.Order"))
            .build();

    var text = EmbeddingTextBuilder.build(document, ALLOW_ALL);

    assertThat(text).isEqualTo("Entity: type\nQualified name: dev.ftathiago.payments.domain.Order");
  }

  @Test
  void build_denyAllPolicyRemovesEverySectionHeaderToo() {
    var document =
        CiirDocument.builder()
            .id("sha256:doesnotmatterforthistest")
            .kind(CiirKind.METHOD)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("m", "a.B.m", "a.B.m()"))
            .addRelation(reads("a.B.field"))
            .build();

    var text = EmbeddingTextBuilder.build(document, DENY_ALL);

    assertThat(text).isEqualTo("Entity: method\nQualified name: a.B.m");
  }

  @Test
  void build_isDeterministic() {
    var document =
        CiirDocument.builder()
            .id("sha256:doesnotmatterforthistest")
            .kind(CiirKind.TYPE)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("B", "a.B", "a.B"))
            .build();

    assertThat(EmbeddingTextBuilder.build(document, ALLOW_ALL))
        .isEqualTo(EmbeddingTextBuilder.build(document, ALLOW_ALL));
  }

  private static CiirRelation reads(String symbol) {
    return relation(CiirRelationKind.READS, symbol);
  }

  private static CiirRelation calls(String symbol) {
    return relation(CiirRelationKind.CALLS, symbol);
  }

  private static CiirRelation throwsRelation(String symbol) {
    return relation(CiirRelationKind.THROWS, symbol);
  }

  private static CiirRelation relation(CiirRelationKind kind, String symbol) {
    return CiirRelation.of(
        kind,
        CiirRelationTarget.of(symbol),
        CiirRelationResolution.of(CiirResolutionStatus.RESOLVED, CiirResolutionOrigin.PROJECT));
  }
}
