package dev.ftathiago.ciir.application.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ftathiago.ciir.application.exceptions.InvalidInputException;
import dev.ftathiago.ciir.application.model.InputType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultInputResolverTest {

  private final DefaultInputResolver resolver = new DefaultInputResolver();

  @Test
  void resolve_directoryInput(@TempDir Path tempDir) throws Exception {
    var input = resolver.resolve(tempDir.toString());

    assertThat(input.type()).isEqualTo(InputType.DIRECTORY);
    assertThat(input.path()).isEqualTo(tempDir.toAbsolutePath().normalize());
  }

  @Test
  void resolve_leafPomIsProject(@TempDir Path tempDir) throws Exception {
    var pom =
        writePom(
            tempDir,
            "leaf",
            """
                <project><modelVersion>4.0.0</modelVersion>
                <artifactId>leaf</artifactId></project>""");

    assertThat(resolver.resolve(pom.toString()).type()).isEqualTo(InputType.PROJECT);
  }

  @Test
  void resolve_aggregatorPomIsAggregator(@TempDir Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("child"));
    var pom =
        writePom(
            tempDir,
            "root",
            """
                <project><modelVersion>4.0.0</modelVersion>
                <artifactId>root</artifactId>
                <modules><module>child</module></modules></project>""");

    assertThat(resolver.resolve(pom.toString()).type()).isEqualTo(InputType.AGGREGATOR);
  }

  @Test
  void resolve_nonexistentPathThrows() {
    assertThatThrownBy(() -> resolver.resolve("/does/not/exist/pom.xml"))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void resolve_unsupportedFileThrows(@TempDir Path tempDir) throws IOException {
    var file = tempDir.resolve("readme.md");
    Files.writeString(file, "not a pom");

    assertThatThrownBy(() -> resolver.resolve(file.toString()))
        .isInstanceOf(InvalidInputException.class);
  }

  private static Path writePom(Path dir, String name, String content) throws IOException {
    var pom = dir.resolve("pom.xml");
    Files.writeString(pom, content);
    return pom;
  }
}
