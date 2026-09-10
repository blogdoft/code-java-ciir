package dev.ftathiago.ciir.serialization.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The single, shared Jackson configuration for every CIIR JSON artifact ({@code ciir.jsonl}, {@code
 * manifest.json}, {@code analysis-report.json}): camelCase field names (Jackson's default — every
 * CIIR record component is already camelCase), {@code NON_EMPTY} inclusion (one mechanism covering
 * both null and empty-collection omission), lowercase-token enum serialization and a fixed UTC
 * timestamp format (see {@link CiirJacksonModule}), and no pretty-printing (both {@code
 * manifest.json} and {@code analysis-report.json} are minified, single-line JSON).
 */
public final class CiirObjectMapperFactory {

  private CiirObjectMapperFactory() {}

  public static ObjectMapper create() {
    var mapper = new ObjectMapper();
    mapper.registerModule(new CiirJacksonModule());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    mapper.disable(SerializationFeature.INDENT_OUTPUT);
    return mapper;
  }
}
