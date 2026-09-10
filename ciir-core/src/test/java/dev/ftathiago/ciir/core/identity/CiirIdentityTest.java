package dev.ftathiago.ciir.core.identity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ftathiago.ciir.core.CiirKind;
import org.junit.jupiter.api.Test;

class CiirIdentityTest {

  @Test
  void buildCanonicalKey_joinsFourComponentsWithPipe() {
    var key =
        CiirIdentity.buildCanonicalKey(
            "java",
            "payments-application",
            CiirKind.METHOD,
            "dev.ftathiago.payments.PaymentService.authorize()");

    assertThat(key)
        .isEqualTo(
            "java|payments-application|method|dev.ftathiago.payments.PaymentService.authorize()");
  }

  @Test
  void computeId_isDeterministic() {
    var id1 = CiirIdentity.computeId("java", "p", CiirKind.TYPE, "a.B");
    var id2 = CiirIdentity.computeId("java", "p", CiirKind.TYPE, "a.B");

    assertThat(id1).isEqualTo(id2);
  }

  @Test
  void computeId_startsWithSha256Prefix() {
    assertThat(CiirIdentity.computeId("java", "p", CiirKind.TYPE, "a.B")).startsWith("sha256:");
  }

  @Test
  void computeId_differentCanonicalSymbolIdentityProducesDifferentId() {
    var overload1 = CiirIdentity.computeId("java", "p", CiirKind.METHOD, "a.B.m(int)");
    var overload2 = CiirIdentity.computeId("java", "p", CiirKind.METHOD, "a.B.m(int,int)");

    assertThat(overload1).isNotEqualTo(overload2);
  }

  @Test
  void computeId_differentKindProducesDifferentId() {
    var type = CiirIdentity.computeId("java", "p", CiirKind.TYPE, "a.B");
    var method = CiirIdentity.computeId("java", "p", CiirKind.METHOD, "a.B");

    assertThat(type).isNotEqualTo(method);
  }

  @Test
  void computeId_differentProjectProducesDifferentId() {
    var p1 = CiirIdentity.computeId("java", "p1", CiirKind.TYPE, "a.B");
    var p2 = CiirIdentity.computeId("java", "p2", CiirKind.TYPE, "a.B");

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  void computeId_differentLanguageProducesDifferentId() {
    var java = CiirIdentity.computeId("java", "p", CiirKind.TYPE, "a.B");
    var csharp = CiirIdentity.computeId("csharp", "p", CiirKind.TYPE, "a.B");

    assertThat(java).isNotEqualTo(csharp);
  }
}
