package dev.ftathiago.ciir.application.ports;

import java.io.IOException;
import java.nio.file.Path;

public interface CiirWriterFactory {

  /** Creates the (single, run-wide) writer for {@code ciir.jsonl} under {@code outputDirectory}. */
  CiirWriter create(Path outputDirectory) throws IOException;
}
