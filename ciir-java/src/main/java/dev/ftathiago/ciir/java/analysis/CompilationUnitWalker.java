package dev.ftathiago.ciir.java.analysis;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import dev.ftathiago.ciir.core.CiirDocument;
import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.embeddingtext.EmbeddingTextBuilder;
import dev.ftathiago.ciir.core.embeddingtext.EmbeddingTextPolicy;
import dev.ftathiago.ciir.core.hashing.Sha256Text;
import dev.ftathiago.ciir.core.identity.CiirIdentity;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.symbols.CiirFieldInfo;
import dev.ftathiago.ciir.core.symbols.CiirMethodInfo;
import dev.ftathiago.ciir.core.symbols.CiirParameter;
import dev.ftathiago.ciir.core.symbols.CiirSymbol;
import dev.ftathiago.ciir.core.symbols.CiirTypeInfo;
import dev.ftathiago.ciir.java.comments.CommentCollector;
import dev.ftathiago.ciir.java.conditions.ConditionExtractor;
import dev.ftathiago.ciir.java.controlflow.ControlFlowCalculator;
import dev.ftathiago.ciir.java.documentation.JavadocMapper;
import dev.ftathiago.ciir.java.relations.RelationExtractor;
import dev.ftathiago.ciir.java.source.SourceLocationFactory;
import dev.ftathiago.ciir.java.symbolnaming.AccessibilityMapper;
import dev.ftathiago.ciir.java.symbolnaming.ModifierMapper;
import dev.ftathiago.ciir.java.symbolnaming.SymbolNaming;
import dev.ftathiago.ciir.java.symbolnaming.TypeKindMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Walks one already-parsed, symbol-resolvable {@link CompilationUnit}, emitting one {@link
 * CiirDocument} per package/type/constructor/method/field declaration. A declaration whose own
 * identity can't be resolved is skipped (logged, not fatal) — the rest of the file, and the rest of
 * the project, are unaffected.
 */
final class CompilationUnitWalker {

  private static final Logger LOG = LoggerFactory.getLogger(CompilationUnitWalker.class);

  private final String projectName;
  private final Path rootDirectory;
  private final RelationExtractor relationExtractor;
  private final EmbeddingTextPolicy embeddingTextPolicy;
  private final boolean includeSource;
  private final Set<String> emittedPackages;

  CompilationUnitWalker(
      String projectName,
      Path rootDirectory,
      RelationExtractor relationExtractor,
      EmbeddingTextPolicy embeddingTextPolicy,
      boolean includeSource,
      Set<String> emittedPackages) {
    this.projectName = projectName;
    this.rootDirectory = rootDirectory;
    this.relationExtractor = relationExtractor;
    this.embeddingTextPolicy = embeddingTextPolicy;
    this.includeSource = includeSource;
    this.emittedPackages = emittedPackages;
  }

  void walk(CompilationUnit compilationUnit, Path filePath, Consumer<CiirDocument> sink)
      throws IOException {
    var sourceLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
    var relativePath = rootDirectory.relativize(filePath).toString();

    compilationUnit
        .getPackageDeclaration()
        .ifPresent(
            pkg -> {
              var packageName = pkg.getNameAsString();
              if (!packageName.isEmpty() && emittedPackages.add(packageName)) {
                sink.accept(namespaceDocument(packageName));
              }
            });

    for (var type : compilationUnit.getTypes()) {
      walkType(type, relativePath, sourceLines, sink);
    }
  }

  private CiirDocument namespaceDocument(String packageName) {
    var id = CiirIdentity.computeId("java", projectName, CiirKind.NAMESPACE, packageName);
    var builder =
        CiirDocument.builder()
            .id(id)
            .kind(CiirKind.NAMESPACE)
            .language("java")
            .project(projectName)
            .symbol(CiirSymbol.of(packageName, packageName, packageName));
    return finish(builder);
  }

  private void walkType(
      TypeDeclaration<?> typeDeclaration,
      String relativePath,
      List<String> sourceLines,
      Consumer<CiirDocument> sink) {
    try {
      var resolved = typeDeclaration.resolve();

      var qualifiedName = SymbolNaming.qualifiedName(resolved);
      var container = SymbolNaming.container(resolved);
      var id = CiirIdentity.computeId("java", projectName, CiirKind.TYPE, qualifiedName);

      var typeInfo =
          new CiirTypeInfo(
              TypeKindMapper.map(typeDeclaration),
              AccessibilityMapper.fromModifiers(typeDeclaration.getModifiers()),
              ModifierMapper.map(typeDeclaration.getModifiers()),
              List.of());

      var builder =
          CiirDocument.builder()
              .id(id)
              .kind(CiirKind.TYPE)
              .language("java")
              .project(projectName)
              .symbol(
                  new CiirSymbol(
                      typeDeclaration.getNameAsString(), qualifiedName, qualifiedName, container))
              .source(
                  SourceLocationFactory.create(
                      typeDeclaration, relativePath, sourceLines, includeSource))
              .documentation(JavadocMapper.map(typeDeclaration.getJavadoc()).orElse(null))
              .comments(CommentCollector.collect(typeDeclaration))
              .relations(
                  relationExtractor.typeRelations(
                      extendedTypesOf(typeDeclaration), implementedTypesOf(typeDeclaration)))
              .type(typeInfo);
      sink.accept(finish(builder));
    } catch (RuntimeException e) {
      LOG.warn(
          "Skipping unresolvable type {}: {}", typeDeclaration.getNameAsString(), e.getMessage());
    }

    for (var member : typeDeclaration.getMembers()) {
      if (member instanceof ConstructorDeclaration constructor) {
        walkConstructor(constructor, relativePath, sourceLines, sink);
      } else if (member instanceof MethodDeclaration method) {
        walkMethod(method, relativePath, sourceLines, sink);
      } else if (member instanceof FieldDeclaration field) {
        walkField(field, relativePath, sourceLines, sink);
      } else if (member instanceof TypeDeclaration<?> nestedType) {
        walkType(nestedType, relativePath, sourceLines, sink);
      }
    }
  }

  private void walkMethod(
      MethodDeclaration method,
      String relativePath,
      List<String> sourceLines,
      Consumer<CiirDocument> sink) {
    try {
      var resolved = method.resolve();

      var qualifiedName = SymbolNaming.qualifiedName(resolved);
      var canonicalName = SymbolNaming.canonicalName(resolved);
      var container = SymbolNaming.container(resolved);
      var id = CiirIdentity.computeId("java", projectName, CiirKind.METHOD, canonicalName);

      var methodInfo =
          new CiirMethodInfo(
              AccessibilityMapper.fromModifiers(method.getModifiers()),
              ModifierMapper.map(method.getModifiers()),
              parametersOf(resolved),
              SymbolNaming.returnType(resolved),
              null);

      var relations = new ArrayList<CiirRelation>();
      method.getBody().ifPresent(body -> relations.addAll(relationExtractor.bodyRelations(body)));
      relationExtractor.overrides(method).ifPresent(relations::add);

      var builder =
          CiirDocument.builder()
              .id(id)
              .kind(CiirKind.METHOD)
              .language("java")
              .project(projectName)
              .symbol(
                  new CiirSymbol(method.getNameAsString(), qualifiedName, canonicalName, container))
              .source(
                  SourceLocationFactory.create(method, relativePath, sourceLines, includeSource))
              .documentation(JavadocMapper.map(method.getJavadoc()).orElse(null))
              .comments(CommentCollector.collect(method))
              .relations(relations)
              .method(methodInfo);
      method
          .getBody()
          .ifPresent(
              body -> {
                builder.conditions(ConditionExtractor.extract(body));
                builder.controlFlow(ControlFlowCalculator.calculate(body));
              });
      sink.accept(finish(builder));
    } catch (RuntimeException e) {
      LOG.warn("Skipping unresolvable method {}: {}", method.getNameAsString(), e.getMessage());
    }
  }

  private void walkConstructor(
      ConstructorDeclaration constructor,
      String relativePath,
      List<String> sourceLines,
      Consumer<CiirDocument> sink) {
    try {
      var resolved = constructor.resolve();

      var qualifiedName = SymbolNaming.qualifiedName(resolved);
      var canonicalName = SymbolNaming.canonicalName(resolved);
      var container = SymbolNaming.container(resolved);
      var id = CiirIdentity.computeId("java", projectName, CiirKind.CONSTRUCTOR, canonicalName);

      var methodInfo =
          new CiirMethodInfo(
              AccessibilityMapper.fromModifiers(constructor.getModifiers()),
              ModifierMapper.map(constructor.getModifiers()),
              parametersOf(resolved),
              null,
              null);

      var relations = new ArrayList<>(relationExtractor.bodyRelations(constructor.getBody()));

      var builder =
          CiirDocument.builder()
              .id(id)
              .kind(CiirKind.CONSTRUCTOR)
              .language("java")
              .project(projectName)
              .symbol(
                  new CiirSymbol(
                      constructor.getNameAsString(), qualifiedName, canonicalName, container))
              .source(
                  SourceLocationFactory.create(
                      constructor, relativePath, sourceLines, includeSource))
              .documentation(JavadocMapper.map(constructor.getJavadoc()).orElse(null))
              .comments(CommentCollector.collect(constructor))
              .relations(relations)
              .conditions(ConditionExtractor.extract(constructor.getBody()))
              .controlFlow(ControlFlowCalculator.calculate(constructor.getBody()))
              .method(methodInfo);
      sink.accept(finish(builder));
    } catch (RuntimeException e) {
      LOG.warn(
          "Skipping unresolvable constructor {}: {}",
          constructor.getNameAsString(),
          e.getMessage());
    }
  }

  private void walkField(
      FieldDeclaration field,
      String relativePath,
      List<String> sourceLines,
      Consumer<CiirDocument> sink) {
    for (var variable : field.getVariables()) {
      try {
        var resolved = variable.resolve();
        if (!resolved.isField()) {
          continue;
        }
        var resolvedField = resolved.asField();

        var qualifiedName = SymbolNaming.qualifiedName(resolvedField);
        var container = SymbolNaming.container(resolvedField);
        var id = CiirIdentity.computeId("java", projectName, CiirKind.FIELD, qualifiedName);

        var fieldInfo =
            new CiirFieldInfo(
                AccessibilityMapper.fromModifiers(field.getModifiers()),
                ModifierMapper.map(field.getModifiers()),
                resolvedField.getType().describe());

        var builder =
            CiirDocument.builder()
                .id(id)
                .kind(CiirKind.FIELD)
                .language("java")
                .project(projectName)
                .symbol(
                    new CiirSymbol(
                        variable.getNameAsString(), qualifiedName, qualifiedName, container))
                .source(
                    SourceLocationFactory.create(
                        variable, relativePath, sourceLines, includeSource))
                .documentation(JavadocMapper.map(field.getJavadoc()).orElse(null))
                .comments(CommentCollector.collect(variable))
                .field(fieldInfo);
        sink.accept(finish(builder));
      } catch (RuntimeException e) {
        LOG.warn("Skipping unresolvable field {}: {}", variable.getNameAsString(), e.getMessage());
      }
    }
  }

  private static List<CiirParameter> parametersOf(
      com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration method) {
    var parameters = new ArrayList<CiirParameter>(method.getNumberOfParams());
    for (int i = 0; i < method.getNumberOfParams(); i++) {
      var parameter = method.getParam(i);
      parameters.add(new CiirParameter(parameter.getName(), parameter.describeType()));
    }
    return parameters;
  }

  private static List<ClassOrInterfaceType> extendedTypesOf(TypeDeclaration<?> type) {
    return type instanceof ClassOrInterfaceDeclaration classOrInterface
        ? classOrInterface.getExtendedTypes()
        : NodeList.nodeList();
  }

  private static List<ClassOrInterfaceType> implementedTypesOf(TypeDeclaration<?> type) {
    if (type instanceof ClassOrInterfaceDeclaration classOrInterface) {
      return classOrInterface.getImplementedTypes();
    }
    if (type instanceof EnumDeclaration enumDeclaration) {
      return enumDeclaration.getImplementedTypes();
    }
    if (type instanceof RecordDeclaration recordDeclaration) {
      return recordDeclaration.getImplementedTypes();
    }
    return NodeList.nodeList();
  }

  private CiirDocument finish(CiirDocument.Builder builder) {
    var draft = builder.build();
    var text = EmbeddingTextBuilder.build(draft, embeddingTextPolicy);
    return builder
        .embeddingText(text)
        .embeddingTextHash(Sha256Text.computePrefixedHash(text))
        .build();
  }
}
