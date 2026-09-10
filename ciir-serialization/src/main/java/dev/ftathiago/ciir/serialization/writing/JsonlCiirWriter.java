package dev.ftathiago.ciir.serialization.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ftathiago.ciir.application.ports.CiirWriter;
import dev.ftathiago.ciir.core.CiirDocument;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes {@code ciir.jsonl}: one JSON object per line, never a JSON array, never buffering more
 * than a single document at a time — the file can grow to millions of records without the process
 * holding them all in memory.
 */
public final class JsonlCiirWriter implements CiirWriter {

  private final ObjectMapper objectMapper;
  private final BufferedWriter writer;

  JsonlCiirWriter(ObjectMapper objectMapper, Path jsonlPath) throws IOException {
    this.objectMapper = objectMapper;
    // UTF-8 with no BOM (java.nio.charset.StandardCharsets.UTF_8 never emits one); the file
    // is always recreated/truncated, never appended to.
    this.writer =
        Files.newBufferedWriter(
            jsonlPath,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
  }

  @Override
  public void write(CiirDocument document) throws IOException {
    writer.write(objectMapper.writeValueAsString(document));
    // Force LF regardless of OS — never writer.newLine(), which is platform-dependent.
    writer.write("\n");
  }

  @Override
  public void close() throws IOException {
    writer.flush();
    writer.close();
  }
}
