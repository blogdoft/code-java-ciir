package dev.ftathiago.ciir.core.controlflow;

/**
 * Aggregate control-flow metrics for a method-like entity's body. All four fields are required
 * whenever this block is attached at all; the document-level {@code controlFlow} property itself is
 * omitted entirely for bodyless/non-method entities.
 */
public record CiirControlFlow(
    int basicBlockCount, int cyclomaticComplexity, boolean hasBranches, boolean hasLoops) {}
