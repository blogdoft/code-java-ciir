package dev.ftathiago.ciir.core.relations;

import dev.ftathiago.ciir.core.source.CiirRange;
import java.util.Objects;

/** One statically observable fact edge from the owning document to a target symbol. */
public record CiirRelation(
    CiirRelationKind kind,
    CiirRelationTarget target,
    CiirRelationResolution resolution,
    CiirRange location) {

  public CiirRelation {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(resolution, "resolution");
  }

  public static CiirRelation of(
      CiirRelationKind kind, CiirRelationTarget target, CiirRelationResolution resolution) {
    return new CiirRelation(kind, target, resolution, null);
  }
}
