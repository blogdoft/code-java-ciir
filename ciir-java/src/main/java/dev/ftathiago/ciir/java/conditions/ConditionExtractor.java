package dev.ftathiago.ciir.java.conditions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import dev.ftathiago.ciir.core.conditions.CiirCondition;
import dev.ftathiago.ciir.core.conditions.CiirConditionKind;
import dev.ftathiago.ciir.java.relations.FieldReads;
import dev.ftathiago.ciir.java.source.SourceLocationFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Preserves branching/looping constructs found in a method body as verbatim facts — never
 * interpreted as business rules. Kinds follow {@code .specs/01-spec-inicial.md}'s "guard"
 * heuristic: a no-{@code else} {@code if} whose body is a single early-exit statement.
 */
public final class ConditionExtractor {

  private ConditionExtractor() {}

  public static List<CiirCondition> extract(Node body) {
    var conditions = new ArrayList<CiirCondition>();

    for (var ifStmt : body.findAll(IfStmt.class)) {
      conditions.add(condition(classifyIf(ifStmt), ifStmt.getCondition(), ifStmt));
    }
    for (var switchStmt : body.findAll(SwitchStmt.class)) {
      conditions.add(condition(CiirConditionKind.SWITCH, switchStmt.getSelector(), switchStmt));
    }
    for (var switchExpr : body.findAll(SwitchExpr.class)) {
      conditions.add(
          condition(CiirConditionKind.SWITCH_EXPRESSION, switchExpr.getSelector(), switchExpr));
    }
    for (var whileStmt : body.findAll(WhileStmt.class)) {
      conditions.add(condition(CiirConditionKind.WHILE, whileStmt.getCondition(), whileStmt));
    }
    for (var doStmt : body.findAll(DoStmt.class)) {
      conditions.add(condition(CiirConditionKind.DO_WHILE, doStmt.getCondition(), doStmt));
    }
    for (var forStmt : body.findAll(ForStmt.class)) {
      Node expression = forStmt.getCompare().map(Node.class::cast).orElse(forStmt);
      conditions.add(condition(CiirConditionKind.FOR, expression, forStmt));
    }
    for (var forEachStmt : body.findAll(ForEachStmt.class)) {
      conditions.add(condition(CiirConditionKind.FOREACH, forEachStmt.getIterable(), forEachStmt));
    }
    for (var conditionalExpr : body.findAll(ConditionalExpr.class)) {
      conditions.add(
          condition(
              CiirConditionKind.CONDITIONAL_EXPRESSION,
              conditionalExpr.getCondition(),
              conditionalExpr));
    }

    return List.copyOf(conditions);
  }

  private static CiirCondition condition(
      CiirConditionKind kind, Node expressionNode, Node locationNode) {
    var reads =
        expressionNode instanceof com.github.javaparser.ast.expr.Expression expression
            ? FieldReads.collect(expression)
            : List.<String>of();
    return new CiirCondition(
        kind, expressionNode.toString(), SourceLocationFactory.rangeOf(locationNode), reads);
  }

  private static CiirConditionKind classifyIf(IfStmt ifStmt) {
    if (isElseIf(ifStmt)) {
      return CiirConditionKind.ELSE_IF;
    }
    return isGuard(ifStmt) ? CiirConditionKind.GUARD : CiirConditionKind.IF;
  }

  private static boolean isElseIf(IfStmt ifStmt) {
    return ifStmt
        .getParentNode()
        .filter(IfStmt.class::isInstance)
        .map(IfStmt.class::cast)
        .flatMap(IfStmt::getElseStmt)
        .filter(elseStmt -> elseStmt == ifStmt)
        .isPresent();
  }

  private static boolean isGuard(IfStmt ifStmt) {
    if (ifStmt.getElseStmt().isPresent()) {
      return false;
    }
    var effectiveThen = singleStatement(ifStmt.getThenStmt());
    return effectiveThen instanceof ReturnStmt
        || effectiveThen instanceof ThrowStmt
        || effectiveThen instanceof ContinueStmt
        || effectiveThen instanceof BreakStmt;
  }

  private static Statement singleStatement(Statement statement) {
    if (statement instanceof BlockStmt block && block.getStatements().size() == 1) {
      return block.getStatements().get(0);
    }
    return statement;
  }
}
