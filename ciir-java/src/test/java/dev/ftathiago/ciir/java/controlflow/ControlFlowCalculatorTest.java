package dev.ftathiago.ciir.java.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;

class ControlFlowCalculatorTest {

  @Test
  void calculate_straightLineBodyHasComplexityOne() {
    var body = StaticJavaParser.parseBlock("{ int x = 1; return x; }");

    var controlFlow = ControlFlowCalculator.calculate(body);

    assertThat(controlFlow.cyclomaticComplexity()).isEqualTo(1);
    assertThat(controlFlow.hasBranches()).isFalse();
    assertThat(controlFlow.hasLoops()).isFalse();
  }

  @Test
  void calculate_ifStatementIncreasesComplexityAndSetsHasBranches() {
    var body = StaticJavaParser.parseBlock("{ if (x > 0) { return 1; } return 0; }");

    var controlFlow = ControlFlowCalculator.calculate(body);

    assertThat(controlFlow.cyclomaticComplexity()).isEqualTo(2);
    assertThat(controlFlow.hasBranches()).isTrue();
    assertThat(controlFlow.hasLoops()).isFalse();
  }

  @Test
  void calculate_loopSetsHasLoops() {
    var body = StaticJavaParser.parseBlock("{ for (int i = 0; i < 10; i++) { } }");

    var controlFlow = ControlFlowCalculator.calculate(body);

    assertThat(controlFlow.hasLoops()).isTrue();
    assertThat(controlFlow.cyclomaticComplexity()).isEqualTo(2);
  }

  @Test
  void calculate_logicalOperatorsCountAsDecisionPoints() {
    var body = StaticJavaParser.parseBlock("{ if (a && b || c) { return 1; } return 0; }");

    var controlFlow = ControlFlowCalculator.calculate(body);

    assertThat(controlFlow.cyclomaticComplexity()).isEqualTo(4);
  }
}
