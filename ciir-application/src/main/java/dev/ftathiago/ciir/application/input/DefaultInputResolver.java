package dev.ftathiago.ciir.application.input;

import dev.ftathiago.ciir.application.exceptions.InvalidInputException;
import dev.ftathiago.ciir.application.model.AnalysisInput;
import dev.ftathiago.ciir.application.model.InputType;
import dev.ftathiago.ciir.application.ports.InputResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the raw CLI path argument into a directory, a leaf module {@code pom.xml}, or an
 * aggregator {@code pom.xml} (one declaring {@code <modules>}).
 */
public final class DefaultInputResolver implements InputResolver {

  @Override
  public AnalysisInput resolve(String rawPath) throws InvalidInputException {
    var path = Path.of(rawPath).toAbsolutePath().normalize();

    if (!Files.exists(path)) {
      throw new InvalidInputException("Path does not exist: " + rawPath);
    }

    if (Files.isDirectory(path)) {
      return new AnalysisInput(InputType.DIRECTORY, path);
    }

    if (!"pom.xml".equals(path.getFileName().toString())) {
      throw new InvalidInputException(
          "Unsupported input: " + rawPath + " (expected a directory or a pom.xml)");
    }

    try {
      var type = PomInspector.isAggregator(path) ? InputType.AGGREGATOR : InputType.PROJECT;
      return new AnalysisInput(type, path);
    } catch (IOException e) {
      throw new InvalidInputException("Failed to read " + rawPath + ": " + e.getMessage(), e);
    }
  }
}
