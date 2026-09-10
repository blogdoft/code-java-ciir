package dev.ftathiago.ciir.serialization.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.symbols.CiirAccessibility;
import dev.ftathiago.ciir.core.symbols.CiirFieldInfo;
import dev.ftathiago.ciir.core.symbols.CiirMethodInfo;
import dev.ftathiago.ciir.core.symbols.CiirSymbol;
import dev.ftathiago.ciir.core.symbols.CiirTypeInfo;
import dev.ftathiago.ciir.core.symbols.CiirTypeKind;
import dev.ftathiago.ciir.serialization.json.CiirObjectMapperFactory;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every record produced by this generator must validate against {@code ciir.schema.json} — the
 * schema is the contract. One valid sample per emitted {@code kind}, plus a handful of
 * deliberately-invalid samples.
 */
class SchemaConformanceTest {

  private static final JsonSchema SCHEMA = loadSchema();
  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
      CiirObjectMapperFactory.create();

  private static JsonSchema loadSchema() {
    var config = new SchemaValidatorsConfig();
    return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        .getSchema(CiirSchemaProvider.getSchemaJson(), config);
  }

  static Stream<CiirDocument> validSamplesPerKind() {
    return Stream.of(
        CiirDocument.builder()
            .id(id("project"))
            .kind(CiirKind.PROJECT)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("p", "p", "p"))
            .build(),
        CiirDocument.builder()
            .id(id("namespace"))
            .kind(CiirKind.NAMESPACE)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("dev.ftathiago", "dev.ftathiago", "dev.ftathiago"))
            .build(),
        CiirDocument.builder()
            .id(id("type"))
            .kind(CiirKind.TYPE)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("Order", "a.Order", "a.Order"))
            .type(CiirTypeInfo.of(CiirTypeKind.CLASS, CiirAccessibility.PUBLIC))
            .build(),
        CiirDocument.builder()
            .id(id("method"))
            .kind(CiirKind.METHOD)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("m", "a.B.m", "a.B.m()"))
            .method(CiirMethodInfo.of(CiirAccessibility.PUBLIC, List.of(), List.of(), "void"))
            .build(),
        CiirDocument.builder()
            .id(id("constructor"))
            .kind(CiirKind.CONSTRUCTOR)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("B", "a.B.<init>", "a.B.<init>()"))
            .method(CiirMethodInfo.of(CiirAccessibility.PUBLIC, List.of(), List.of(), null))
            .build(),
        CiirDocument.builder()
            .id(id("field"))
            .kind(CiirKind.FIELD)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("total", "a.B.total", "a.B.total"))
            .field(CiirFieldInfo.of(CiirAccessibility.PRIVATE, "java.math.BigDecimal"))
            .build());
  }

  @ParameterizedTest
  @MethodSource("validSamplesPerKind")
  void validSample_hasNoSchemaViolations(CiirDocument document) throws Exception {
    var errors = SCHEMA.validate(toJsonNode(document));
    assertThat(errors).isEmpty();
  }

  @org.junit.jupiter.api.Test
  void invalidSample_missingId_isRejected() throws Exception {
    var json = MAPPER.createObjectNode();
    json.put("schemaVersion", "1.0");
    json.put("kind", "type");
    json.put("language", "java");
    json.put("project", "p");
    json.putObject("symbol")
        .put("name", "A")
        .put("qualifiedName", "a.A")
        .put("canonicalName", "a.A");

    assertThat(SCHEMA.validate(json)).isNotEmpty();
  }

  @org.junit.jupiter.api.Test
  void invalidSample_unknownKind_isRejected() throws Exception {
    var document =
        CiirDocument.builder()
            .id(id("bad"))
            .kind(CiirKind.TYPE)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("A", "a.A", "a.A"))
            .type(CiirTypeInfo.of(CiirTypeKind.CLASS, CiirAccessibility.PUBLIC))
            .build();
    var json = (com.fasterxml.jackson.databind.node.ObjectNode) toJsonNode(document);
    json.put("kind", "not-a-real-kind");

    assertThat(SCHEMA.validate(json)).isNotEmpty();
  }

  @org.junit.jupiter.api.Test
  void invalidSample_typeKindMissingTypeBlock_isRejected() throws Exception {
    var document =
        CiirDocument.builder()
            .id(id("bad"))
            .kind(CiirKind.TYPE)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("A", "a.A", "a.A"))
            .build();

    assertThat(SCHEMA.validate(toJsonNode(document))).isNotEmpty();
  }

  @org.junit.jupiter.api.Test
  void invalidSample_unknownTopLevelProperty_isRejected() throws Exception {
    var document =
        CiirDocument.builder()
            .id(id("bad"))
            .kind(CiirKind.PROJECT)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("p", "p", "p"))
            .build();
    var json = (com.fasterxml.jackson.databind.node.ObjectNode) toJsonNode(document);
    json.put("notPartOfTheContract", true);

    assertThat(SCHEMA.validate(json)).isNotEmpty();
  }

  private static JsonNode toJsonNode(CiirDocument document) throws Exception {
    return MAPPER.readTree(MAPPER.writeValueAsString(document));
  }

  private static String id(String seed) {
    return dev.ftathiago.ciir.core.hashing.Sha256Text.computePrefixedHash(seed);
  }
}
