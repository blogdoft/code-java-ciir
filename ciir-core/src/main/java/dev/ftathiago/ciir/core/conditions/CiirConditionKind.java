package dev.ftathiago.ciir.core.conditions;

/**
 * The kind of branching/looping construct preserved. {@code GUARD} is used specifically for an
 * {@code if} with no {@code else} whose body is a single {@code return}/{@code throw}/ {@code
 * continue}/{@code break} statement (an early-exit guard clause); every other {@code if} is {@code
 * IF} (or {@code ELSE_IF} when it is the {@code else} branch of another {@code if}). {@code
 * SWITCH_EXPRESSION} covers Java 14+ arrow-form {@code switch} expressions; {@code FOREACH} covers
 * Java's enhanced {@code for}.
 */
public enum CiirConditionKind {
  IF,
  ELSE_IF,
  SWITCH,
  SWITCH_EXPRESSION,
  WHILE,
  DO_WHILE,
  FOR,
  FOREACH,
  CONDITIONAL_EXPRESSION,
  GUARD
}
