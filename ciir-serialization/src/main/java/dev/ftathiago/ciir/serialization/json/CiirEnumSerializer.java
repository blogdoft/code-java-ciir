package dev.ftathiago.ciir.serialization.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dev.ftathiago.ciir.core.documentation.CiirDocumentationFormat;
import java.io.IOException;
import java.util.Locale;

/**
 * Serializes every CIIR enum as its lowercase, underscore-separated token (enum constant names
 * already use {@code UPPER_SNAKE_CASE}, so {@code name().toLowerCase()} already produces the
 * schema's {@code snake_case} tokens — e.g. {@code PACKAGE_PRIVATE} → {@code "package_private"}).
 *
 * <p>The one exception is {@link CiirDocumentationFormat#XML_DOC}, whose schema token is the
 * hyphenated {@code "xml-doc"} — reserved for other language generators, but handled correctly here
 * for completeness/round-tripping.
 */
final class CiirEnumSerializer extends StdSerializer<Enum<?>> {

  @SuppressWarnings({"unchecked", "rawtypes"})
  CiirEnumSerializer() {
    super((Class) Enum.class);
  }

  @Override
  public void serialize(Enum<?> value, JsonGenerator generator, SerializerProvider provider)
      throws IOException {
    generator.writeString(toToken(value));
  }

  private static String toToken(Enum<?> value) {
    if (value == CiirDocumentationFormat.XML_DOC) {
      return "xml-doc";
    }
    return value.name().toLowerCase(Locale.ROOT);
  }
}
