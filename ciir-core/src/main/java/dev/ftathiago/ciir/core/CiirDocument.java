package dev.ftathiago.ciir.core;

import dev.ftathiago.ciir.core.comments.CiirComment;
import dev.ftathiago.ciir.core.conditions.CiirCondition;
import dev.ftathiago.ciir.core.controlflow.CiirControlFlow;
import dev.ftathiago.ciir.core.documentation.CiirDocumentation;
import dev.ftathiago.ciir.core.embeddingtext.EmbeddingTextBuilder;
import dev.ftathiago.ciir.core.extensions.CiirExtensions;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.source.CiirSourceLocation;
import dev.ftathiago.ciir.core.symbols.CiirFieldInfo;
import dev.ftathiago.ciir.core.symbols.CiirMethodInfo;
import dev.ftathiago.ciir.core.symbols.CiirSymbol;
import dev.ftathiago.ciir.core.symbols.CiirTypeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The root CIIR envelope — the entire shape of one JSON Lines record. Properties with no content
 * are meant to be omitted at serialization time rather than written as empty structures; that
 * omission rule is enforced by the serialization layer (see {@code ciir-serialization}), not here.
 */
public record CiirDocument(
    String schemaVersion,
    String id,
    CiirKind kind,
    String language,
    String project,
    CiirSymbol symbol,
    CiirSourceLocation source,
    List<CiirSourceLocation> additionalSourceLocations,
    CiirDocumentation documentation,
    List<CiirComment> comments,
    List<CiirRelation> relations,
    List<CiirCondition> conditions,
    CiirControlFlow controlFlow,
    CiirTypeInfo type,
    CiirMethodInfo method,
    CiirFieldInfo field,
    String embeddingText,
    String embeddingTextStrategy,
    String embeddingTextHash,
    CiirExtensions extensions) {

  public CiirDocument {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(language, "language");
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(symbol, "symbol");

    schemaVersion = schemaVersion == null ? SchemaVersion.CURRENT : schemaVersion;
    embeddingTextStrategy =
        embeddingTextStrategy == null ? EmbeddingTextBuilder.STRATEGY_NAME : embeddingTextStrategy;
    additionalSourceLocations =
        additionalSourceLocations == null ? List.of() : List.copyOf(additionalSourceLocations);
    comments = comments == null ? List.of() : List.copyOf(comments);
    relations = relations == null ? List.of() : List.copyOf(relations);
    conditions = conditions == null ? List.of() : List.copyOf(conditions);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent construction for a document's many optional fields. */
  public static final class Builder {

    private String id;
    private CiirKind kind;
    private String language;
    private String project;
    private CiirSymbol symbol;
    private CiirSourceLocation source;
    private final List<CiirSourceLocation> additionalSourceLocations = new ArrayList<>();
    private CiirDocumentation documentation;
    private final List<CiirComment> comments = new ArrayList<>();
    private final List<CiirRelation> relations = new ArrayList<>();
    private final List<CiirCondition> conditions = new ArrayList<>();
    private CiirControlFlow controlFlow;
    private CiirTypeInfo type;
    private CiirMethodInfo method;
    private CiirFieldInfo field;
    private String embeddingText;
    private String embeddingTextHash;
    private CiirExtensions extensions;

    private Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder kind(CiirKind kind) {
      this.kind = kind;
      return this;
    }

    public Builder language(String language) {
      this.language = language;
      return this;
    }

    public Builder project(String project) {
      this.project = project;
      return this;
    }

    public Builder symbol(CiirSymbol symbol) {
      this.symbol = symbol;
      return this;
    }

    public Builder source(CiirSourceLocation source) {
      this.source = source;
      return this;
    }

    public Builder additionalSourceLocations(List<CiirSourceLocation> locations) {
      this.additionalSourceLocations.addAll(locations);
      return this;
    }

    public Builder documentation(CiirDocumentation documentation) {
      this.documentation = documentation;
      return this;
    }

    public Builder comments(List<CiirComment> comments) {
      this.comments.addAll(comments);
      return this;
    }

    public Builder relations(List<CiirRelation> relations) {
      this.relations.addAll(relations);
      return this;
    }

    public Builder addRelation(CiirRelation relation) {
      this.relations.add(relation);
      return this;
    }

    public Builder conditions(List<CiirCondition> conditions) {
      this.conditions.addAll(conditions);
      return this;
    }

    public Builder controlFlow(CiirControlFlow controlFlow) {
      this.controlFlow = controlFlow;
      return this;
    }

    public Builder type(CiirTypeInfo type) {
      this.type = type;
      return this;
    }

    public Builder method(CiirMethodInfo method) {
      this.method = method;
      return this;
    }

    public Builder field(CiirFieldInfo field) {
      this.field = field;
      return this;
    }

    public Builder embeddingText(String embeddingText) {
      this.embeddingText = embeddingText;
      return this;
    }

    public Builder embeddingTextHash(String embeddingTextHash) {
      this.embeddingTextHash = embeddingTextHash;
      return this;
    }

    public Builder extensions(CiirExtensions extensions) {
      this.extensions = extensions;
      return this;
    }

    public CiirDocument build() {
      return new CiirDocument(
          SchemaVersion.CURRENT,
          id,
          kind,
          language,
          project,
          symbol,
          source,
          additionalSourceLocations,
          documentation,
          comments,
          relations,
          conditions,
          controlFlow,
          type,
          method,
          field,
          embeddingText,
          EmbeddingTextBuilder.STRATEGY_NAME,
          embeddingTextHash,
          extensions);
    }
  }
}
