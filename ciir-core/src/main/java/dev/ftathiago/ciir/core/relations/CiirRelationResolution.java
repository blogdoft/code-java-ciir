package dev.ftathiago.ciir.core.relations;

import java.util.Objects;

/**
 * @param reason populated for unresolved/ambiguous/dynamic relations; never fabricated.
 */
public record CiirRelationResolution(
    CiirResolutionStatus status, CiirResolutionOrigin origin, String reason) {

  public CiirRelationResolution {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(origin, "origin");
  }

  public static CiirRelationResolution of(
      CiirResolutionStatus status, CiirResolutionOrigin origin) {
    return new CiirRelationResolution(status, origin, null);
  }
}
