package dev.ftathiago.ciir.application.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ftathiago.ciir.application.model.AnalysisInput;
import dev.ftathiago.ciir.application.model.InputType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectDiscoveryServiceTest {

  private final ProjectDiscoveryService discovery = new ProjectDiscoveryService();

  @Test
  void discover_projectInput_returnsSingleNormalizedPath(@TempDir Path tempDir) throws IOException {
    var pom = leafPom(tempDir, "leaf");

    var result = discovery.discover(new AnalysisInput(InputType.PROJECT, pom));

    assertThat(result).containsExactly(pom.toAbsolutePath().normalize());
  }

  @Test
  void discover_aggregatorInput_listsTransitiveLeafModules(@TempDir Path tempDir)
      throws IOException {
    Files.createDirectories(tempDir.resolve("a"));
    Files.createDirectories(tempDir.resolve("b"));
    leafPom(tempDir.resolve("a"), "a");
    leafPom(tempDir.resolve("b"), "b");
    var aggregator = aggregatorPom(tempDir, "root", "a", "b");

    var result = discovery.discover(new AnalysisInput(InputType.AGGREGATOR, aggregator));

    assertThat(result)
        .containsExactlyInAnyOrder(
            tempDir.resolve("a/pom.xml").toAbsolutePath().normalize(),
            tempDir.resolve("b/pom.xml").toAbsolutePath().normalize());
  }

  @Test
  void discover_directoryInput_dedupesModulesFoundBothDirectlyAndViaAggregator(
      @TempDir Path tempDir) throws IOException {
    // root/pom.xml (aggregator) -> src/domain, src/application
    Files.createDirectories(tempDir.resolve("src/domain"));
    Files.createDirectories(tempDir.resolve("src/application"));
    leafPom(tempDir.resolve("src/domain"), "domain");
    leafPom(tempDir.resolve("src/application"), "application");
    aggregatorPom(tempDir, "app", "src/domain", "src/application");

    // services/billing/pom.xml (aggregator) -> billing-domain
    Files.createDirectories(tempDir.resolve("services/billing/billing-domain"));
    leafPom(tempDir.resolve("services/billing/billing-domain"), "billing-domain");
    aggregatorPom(tempDir.resolve("services/billing"), "billing", "billing-domain");

    // tools/tool/pom.xml — a leaf, unreferenced by any aggregator
    Files.createDirectories(tempDir.resolve("tools/tool"));
    leafPom(tempDir.resolve("tools/tool"), "tool");

    var result = discovery.discover(new AnalysisInput(InputType.DIRECTORY, tempDir));

    assertThat(result)
        .containsExactlyInAnyOrder(
            tempDir.resolve("src/domain/pom.xml").toAbsolutePath().normalize(),
            tempDir.resolve("src/application/pom.xml").toAbsolutePath().normalize(),
            tempDir.resolve("services/billing/billing-domain/pom.xml").toAbsolutePath().normalize(),
            tempDir.resolve("tools/tool/pom.xml").toAbsolutePath().normalize());
  }

  @Test
  void discover_directoryInput_skipsIgnoredDirectories(@TempDir Path tempDir) throws IOException {
    Files.createDirectories(tempDir.resolve("src"));
    leafPom(tempDir.resolve("src"), "real");

    Files.createDirectories(tempDir.resolve("target"));
    leafPom(tempDir.resolve("target"), "generated");
    Files.createDirectories(tempDir.resolve(".git"));
    leafPom(tempDir.resolve(".git"), "fake-git");
    Files.createDirectories(tempDir.resolve(".idea"));
    leafPom(tempDir.resolve(".idea"), "fake-idea");
    Files.createDirectories(tempDir.resolve(".settings"));
    leafPom(tempDir.resolve(".settings"), "fake-settings");

    var result = discovery.discover(new AnalysisInput(InputType.DIRECTORY, tempDir));

    assertThat(result).containsExactly(tempDir.resolve("src/pom.xml").toAbsolutePath().normalize());
  }

  @Test
  void discover_isDeterministicAndSorted(@TempDir Path tempDir) throws IOException {
    Files.createDirectories(tempDir.resolve("zebra"));
    Files.createDirectories(tempDir.resolve("alpha"));
    leafPom(tempDir.resolve("zebra"), "zebra");
    leafPom(tempDir.resolve("alpha"), "alpha");

    var first = discovery.discover(new AnalysisInput(InputType.DIRECTORY, tempDir));
    var second = discovery.discover(new AnalysisInput(InputType.DIRECTORY, tempDir));

    assertThat(first).isEqualTo(second).isSorted();
  }

  private static Path leafPom(Path dir, String artifactId) throws IOException {
    var pom = dir.resolve("pom.xml");
    Files.writeString(
        pom,
        """
                <project><modelVersion>4.0.0</modelVersion>
                <artifactId>%s</artifactId></project>"""
            .formatted(artifactId));
    return pom;
  }

  private static Path aggregatorPom(Path dir, String artifactId, String... modules)
      throws IOException {
    var moduleXml = new StringBuilder();
    for (var module : modules) {
      moduleXml.append("<module>").append(module).append("</module>");
    }
    var pom = dir.resolve("pom.xml");
    Files.writeString(
        pom,
        """
                <project><modelVersion>4.0.0</modelVersion>
                <artifactId>%s</artifactId>
                <packaging>pom</packaging>
                <modules>%s</modules></project>"""
            .formatted(artifactId, moduleXml));
    return pom;
  }
}
