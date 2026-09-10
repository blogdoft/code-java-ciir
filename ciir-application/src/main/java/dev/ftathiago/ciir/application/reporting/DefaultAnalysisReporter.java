package dev.ftathiago.ciir.application.reporting;

import dev.ftathiago.ciir.application.model.AnalysisError;
import dev.ftathiago.ciir.application.model.AnalysisReport;
import dev.ftathiago.ciir.application.ports.AnalysisReporter;
import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.relations.CiirResolutionStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete accumulator for one run's operational facts.
 *
 * <p>{@code documents.filesAnalyzed} is derived from the distinct set of {@code
 * document.source().path()} values seen, rather than requiring a separate reporting hook from the
 * analyzer. A relation counts as "resolved" for report purposes when its target was successfully
 * classified at all — {@code RESOLVED} (in-run) or {@code EXTERNAL} (JDK/dependency, a normal,
 * expected outcome) — and "unresolved" only when it genuinely could not be classified (@code
 * UNRESOLVED}/{@code AMBIGUOUS}/{@code DYNAMIC}).
 */
public final class DefaultAnalysisReporter implements AnalysisReporter {

  private int discovered;
  private int analyzed;
  private int failed;
  private int documentsAnalyzed;
  private int types;
  private int methods;
  private int relationsResolved;
  private int relationsUnresolved;
  private final Set<String> filesAnalyzed = new HashSet<>();
  private final List<AnalysisError> errors = new ArrayList<>();

  @Override
  public void recordProjectDiscovered(Path projectPath) {
    discovered++;
  }

  @Override
  public void recordProjectAnalyzed(Path projectPath) {
    analyzed++;
  }

  @Override
  public void recordProjectFailed(Path projectPath, String message, String category) {
    failed++;
    errors.add(new AnalysisError(projectPath.toString(), message, category));
  }

  @Override
  public void recordDocument(CiirDocument document) {
    documentsAnalyzed++;

    if (document.kind() == CiirKind.TYPE) {
      types++;
    } else if (document.kind() == CiirKind.METHOD || document.kind() == CiirKind.CONSTRUCTOR) {
      methods++;
    }

    if (document.source() != null) {
      filesAnalyzed.add(document.source().path());
    }

    for (var relation : document.relations()) {
      var status = relation.resolution().status();
      if (status == CiirResolutionStatus.RESOLVED || status == CiirResolutionStatus.EXTERNAL) {
        relationsResolved++;
      } else {
        relationsUnresolved++;
      }
    }
  }

  @Override
  public AnalysisReport buildReport() {
    return new AnalysisReport(
        failed == 0,
        new AnalysisReport.ProjectCounts(discovered, analyzed, failed),
        new AnalysisReport.DocumentCounts(documentsAnalyzed, 0, filesAnalyzed.size()),
        new AnalysisReport.EntityCounts(types, methods),
        new AnalysisReport.RelationCounts(relationsResolved, relationsUnresolved),
        List.copyOf(errors));
  }
}
