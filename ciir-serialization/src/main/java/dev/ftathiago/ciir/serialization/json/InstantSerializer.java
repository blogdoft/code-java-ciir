package dev.ftathiago.ciir.serialization.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formats {@link Instant} as {@code yyyy-MM-dd'T'HH:mm:ss'Z'} in UTC — deliberately not Jackson's
 * default ISO-8601 representation, which may include fractional seconds or a {@code +00:00} offset
 * instead of a literal {@code Z}.
 */
final class InstantSerializer extends StdSerializer<Instant> {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  InstantSerializer() {
    super(Instant.class);
  }

  @Override
  public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    generator.writeString(FORMATTER.format(value));
  }
}
