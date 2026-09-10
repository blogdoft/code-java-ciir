package dev.ftathiago.ciir.java.controlflow;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import dev.ftathiago.ciir.core.controlflow.CiirControlFlow;

/**
 * Computes {@code CiirControlFlow}'s aggregate metrics by direct AST decision-point counting.
 * <b>This is a deliberate approximation</b>, not a real control-flow graph: JavaParser has no API
 * analogous to Roslyn's {@code FlowAnalysis.ControlFlowGraph}. See {@code
 * .specs/01-spec-inicial.md}'s "Control flow" section.
 */
public final class ControlFlowCalculator {

  private ControlFlowCalculator() {}

  public static CiirControlFlow calculate(Node body) {
    int branchPoints =
        body.findAll(IfStmt.class).size()
            + nonDefaultSwitchEntries(body)
            + body.findAll(ConditionalExpr.class).size()
            + body.findAll(CatchClause.class).size()
            + logicalOperators(body);

    int loopPoints =
        body.findAll(ForStmt.class).size()
            + body.findAll(ForEachStmt.class).size()
            + body.findAll(WhileStmt.class).size()
            + body.findAll(DoStmt.class).size();

    int decisionPoints = branchPoints + loopPoints;
    return new CiirControlFlow(
        decisionPoints + 1, decisionPoints + 1, branchPoints > 0, loopPoints > 0);
  }

  private static int nonDefaultSwitchEntries(Node body) {
    return (int)
        body.findAll(SwitchEntry.class).stream()
            .filter(entry -> !entry.getLabels().isEmpty())
            .count();
  }

  private static int logicalOperators(Node body) {
    return (int)
        body.findAll(BinaryExpr.class).stream()
            .filter(
                expr ->
                    expr.getOperator() == BinaryExpr.Operator.AND
                        || expr.getOperator() == BinaryExpr.Operator.OR)
            .count();
  }
}
