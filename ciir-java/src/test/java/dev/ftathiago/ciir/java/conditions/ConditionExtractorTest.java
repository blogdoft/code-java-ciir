package dev.ftathiago.ciir.java.conditions;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import dev.ftathiago.ciir.core.conditions.CiirConditionKind;
import org.junit.jupiter.api.Test;

class ConditionExtractorTest {

  @Test
  void extract_plainIf() {
    var body = StaticJavaParser.parseBlock("{ if (x > 0) { doSomething(); } }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.IF);
    assertThat(conditions.get(0).expression()).isEqualTo("x > 0");
  }

  @Test
  void extract_guardClause_singleReturnStatementNoElse() {
    var body = StaticJavaParser.parseBlock("{ if (x <= 0) { return; } doSomething(); }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.GUARD);
  }

  @Test
  void extract_guardClauseWithoutBraces() {
    var body =
        StaticJavaParser.parseBlock(
            "{ if (x <= 0) throw new IllegalArgumentException(); doSomething(); }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.GUARD);
  }

  @Test
  void extract_elseIfChain() {
    var body =
        StaticJavaParser.parseBlock(
            "{ if (x > 0) { a(); } else if (x < 0) { b(); } else { c(); } }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions)
        .extracting("kind")
        .containsExactly(CiirConditionKind.IF, CiirConditionKind.ELSE_IF);
  }

  @Test
  void extract_ifWithElseIsNotAGuard() {
    var body = StaticJavaParser.parseBlock("{ if (x > 0) { return; } else { return; } }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.IF);
  }

  @Test
  void extract_whileLoop() {
    var body = StaticJavaParser.parseBlock("{ while (x > 0) { x--; } }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.WHILE);
  }

  @Test
  void extract_forEach() {
    var body = StaticJavaParser.parseBlock("{ for (var item : items) { use(item); } }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.FOREACH);
  }

  @Test
  void extract_conditionalExpression() {
    var body = StaticJavaParser.parseBlock("{ int y = x > 0 ? 1 : -1; }");

    var conditions = ConditionExtractor.extract(body);

    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(0).kind()).isEqualTo(CiirConditionKind.CONDITIONAL_EXPRESSION);
  }
}
