package dev.ftathiago.ciir.serialization.writing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.relations.CiirRelationKind;
import dev.ftathiago.ciir.core.relations.CiirRelationResolution;
import dev.ftathiago.ciir.core.relations.CiirRelationTarget;
import dev.ftathiago.ciir.core.relations.CiirResolutionOrigin;
import dev.ftathiago.ciir.core.relations.CiirResolutionStatus;
import dev.ftathiago.ciir.core.symbols.CiirSymbol;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonlCiirWriterTest {

  private final JsonlCiirWriterFactory factory = new JsonlCiirWriterFactory();

  @Test
  void write_producesOneJsonObjectPerLfTerminatedLine(@TempDir Path outputDir) throws Exception {
    try (var writer = factory.create(outputDir)) {
      writer.write(typeDocument("a.A"));
      writer.write(typeDocument("a.B"));
    }

    var content = Files.readString(outputDir.resolve("ciir.jsonl"));
    assertThat(content).doesNotContain("\r\n");
    var lines = content.split("\n", -1);
    assertThat(lines).hasSize(3); // 2 documents + trailing empty string after final \n
    assertThat(lines[2]).isEmpty();
    assertThat(lines[0]).startsWith("{").endsWith("}");
    assertThat(lines[1]).startsWith("{").endsWith("}");
  }

  @Test
  void write_usesCamelCasePropertyNamesAndLowercaseEnumTokens(@TempDir Path outputDir)
      throws Exception {
    try (var writer = factory.create(outputDir)) {
      writer.write(typeDocument("a.A"));
    }

    var line = Files.readString(outputDir.resolve("ciir.jsonl")).lines().findFirst().orElseThrow();
    assertThat(line).contains("\"schemaVersion\"", "\"qualifiedName\"", "\"canonicalName\"");
    assertThat(line).contains("\"kind\":\"type\"");
  }

  @Test
  void write_omitsEmptyCollectionsAndNullOptionalObjects(@TempDir Path outputDir) throws Exception {
    try (var writer = factory.create(outputDir)) {
      writer.write(typeDocument("a.A"));
    }

    var line = Files.readString(outputDir.resolve("ciir.jsonl")).lines().findFirst().orElseThrow();
    assertThat(line)
        .doesNotContain(
            "\"comments\"", "\"relations\"", "\"conditions\"", "\"documentation\"", "\"source\"");
  }

  @Test
  void write_includesNonEmptyRelations(@TempDir Path outputDir) throws Exception {
    var document =
        CiirDocument.builder()
            .id("sha256:" + "0".repeat(64))
            .kind(CiirKind.METHOD)
            .language("java")
            .project("p")
            .symbol(CiirSymbol.of("m", "a.B.m", "a.B.m()"))
            .addRelation(
                CiirRelation.of(
                    CiirRelationKind.CALLS,
                    CiirRelationTarget.of("a.C.n"),
                    CiirRelationResolution.of(
                        CiirResolutionStatus.RESOLVED, CiirResolutionOrigin.PROJECT)))
            .build();

    try (var writer = factory.create(outputDir)) {
      writer.write(document);
    }

    var line = Files.readString(outputDir.resolve("ciir.jsonl")).lines().findFirst().orElseThrow();
    assertThat(line).contains("\"relations\"", "\"status\":\"resolved\"", "\"origin\":\"project\"");
  }

  @Test
  void write_recreatesFileRatherThanAppending(@TempDir Path outputDir) throws Exception {
    try (var writer = factory.create(outputDir)) {
      writer.write(typeDocument("a.A"));
      writer.write(typeDocument("a.B"));
    }
    try (var writer = factory.create(outputDir)) {
      writer.write(typeDocument("a.C"));
    }

    var content = Files.readString(outputDir.resolve("ciir.jsonl"));
    assertThat(content).contains("a.C").doesNotContain("a.A", "a.B");
  }

  @Test
  void write_isDeterministic(@TempDir Path outputDir1, @TempDir Path outputDir2) throws Exception {
    var document = typeDocument("a.A");

    try (var writer = factory.create(outputDir1)) {
      writer.write(document);
    }
    try (var writer = factory.create(outputDir2)) {
      writer.write(document);
    }

    assertThat(Files.readString(outputDir1.resolve("ciir.jsonl")))
        .isEqualTo(Files.readString(outputDir2.resolve("ciir.jsonl")));
  }

  private static CiirDocument typeDocument(String qualifiedName) {
    return CiirDocument.builder()
        .id("sha256:" + "0".repeat(64))
        .kind(CiirKind.TYPE)
        .language("java")
        .project("p")
        .symbol(CiirSymbol.of(qualifiedName, qualifiedName, qualifiedName))
        .build();
  }
}
