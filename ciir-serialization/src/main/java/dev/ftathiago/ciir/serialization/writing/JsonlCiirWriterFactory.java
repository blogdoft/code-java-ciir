package dev.ftathiago.ciir.serialization.writing;

import dev.ftathiago.ciir.application.ports.CiirWriter;
import dev.ftathiago.ciir.application.ports.CiirWriterFactory;
import dev.ftathiago.ciir.serialization.json.CiirObjectMapperFactory;
import java.io.IOException;
import java.nio.file.Path;

public final class JsonlCiirWriterFactory implements CiirWriterFactory {

  @Override
  public CiirWriter create(Path outputDirectory) throws IOException {
    return new JsonlCiirWriter(
        CiirObjectMapperFactory.create(), outputDirectory.resolve("ciir.jsonl"));
  }
}
