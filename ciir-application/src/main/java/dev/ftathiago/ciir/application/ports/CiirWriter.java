package dev.ftathiago.ciir.application.ports;

import dev.ftathiago.ciir.core.CiirDocument;
import java.io.Closeable;
import java.io.IOException;

/** Writes one CIIR document at a time to {@code ciir.jsonl}, never buffering the whole file. */
public interface CiirWriter extends Closeable {

  void write(CiirDocument document) throws IOException;
}
