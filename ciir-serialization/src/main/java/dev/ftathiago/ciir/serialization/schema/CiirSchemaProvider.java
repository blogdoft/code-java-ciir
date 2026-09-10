package dev.ftathiago.ciir.serialization.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Provides the packaged {@code ciir.schema.json}. The canonical file lives once at the repo root
 * ({@code schemas/ciir.schema.json}) and is copied onto this module's classpath at build time (see
 * {@code ciir-serialization/pom.xml}'s {@code <resources>}) — a single source of truth, never a
 * second checked-in copy.
 */
public final class CiirSchemaProvider {

  private static final String RESOURCE_PATH = "ciir/ciir.schema.json";

  private CiirSchemaProvider() {}

  public static String getSchemaJson() {
    try (InputStream stream =
        CiirSchemaProvider.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
      if (stream == null) {
        throw new IllegalStateException("Packaged schema resource not found: " + RESOURCE_PATH);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
