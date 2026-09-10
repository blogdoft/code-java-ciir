package dev.ftathiago.ciir.java.relations;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import dev.ftathiago.ciir.core.CiirKind;
import dev.ftathiago.ciir.core.identity.CiirIdentity;
import dev.ftathiago.ciir.core.relations.CiirRelation;
import dev.ftathiago.ciir.core.relations.CiirRelationKind;
import dev.ftathiago.ciir.core.relations.CiirRelationResolution;
import dev.ftathiago.ciir.core.relations.CiirRelationTarget;
import dev.ftathiago.ciir.core.relations.CiirResolutionOrigin;
import dev.ftathiago.ciir.core.relations.CiirResolutionStatus;
import dev.ftathiago.ciir.core.source.CiirRange;
import dev.ftathiago.ciir.java.source.SourceLocationFactory;
import dev.ftathiago.ciir.java.symbolnaming.SymbolNaming;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts every statically observable {@link CiirRelation} from a type's supertype clauses and
 * from a method/constructor body, using JavaParser's Symbol Solver — never inferring a relation
 * from source text alone. A resolution failure never fabricates a fact: it is recorded as an {@code
 * unresolved} relation (for explicit invocations/type references) or, for the much noisier
 * plain-name read/write scan, simply skipped (most failures there are ordinary local
 * variables/parameters, not errors).
 */
public final class RelationExtractor {

  private final ResolutionClassifier classifier;

  public RelationExtractor(ResolutionClassifier classifier) {
    this.classifier = classifier;
  }

  public List<CiirRelation> typeRelations(
      List<ClassOrInterfaceType> extendedTypes, List<ClassOrInterfaceType> implementedTypes) {
    var relations = new ArrayList<CiirRelation>();
    extendedTypes.forEach(
        type -> typeReferenceRelation(CiirRelationKind.INHERITS, type).ifPresent(relations::add));
    implementedTypes.forEach(
        type -> typeReferenceRelation(CiirRelationKind.IMPLEMENTS, type).ifPresent(relations::add));
    return relations;
  }

  /** The immediately overridden method up the resolved supertype chain, when one exists. */
  public Optional<CiirRelation> overrides(MethodDeclaration methodDeclaration) {
    try {
      var resolvedMethod = methodDeclaration.resolve();
      for (var ancestor : resolvedMethod.declaringType().getAllAncestors()) {
        var ancestorDeclaration = ancestor.getTypeDeclaration();
        if (ancestorDeclaration.isEmpty()) {
          continue;
        }
        for (var candidate : ancestorDeclaration.get().getDeclaredMethods()) {
          if (isSameSignature(resolvedMethod, candidate)) {
            return Optional.of(
                resolvedRelation(
                    CiirRelationKind.OVERRIDES,
                    candidate,
                    SymbolNaming.qualifiedName(candidate),
                    SymbolNaming.canonicalName(candidate),
                    CiirKind.METHOD,
                    SourceLocationFactory.rangeOf(methodDeclaration.getName())));
          }
        }
      }
      return Optional.empty();
    } catch (RuntimeException e) {
      // Cannot reliably prove an override without a resolved supertype chain — never guess.
      return Optional.empty();
    }
  }

  public List<CiirRelation> bodyRelations(Node body) {
    var relations = new ArrayList<CiirRelation>();
    Set<Node> assignmentTargets = Collections.newSetFromMap(new IdentityHashMap<>());

    for (var call : body.findAll(MethodCallExpr.class)) {
      relations.add(callRelation(call));
    }
    for (var creation : body.findAll(ObjectCreationExpr.class)) {
      relations.add(constructsRelation(creation));
    }
    for (var assign : body.findAll(AssignExpr.class)) {
      assignmentTargets.add(assign.getTarget());
      writeRelation(assign.getTarget()).ifPresent(relations::add);
    }
    for (var nameExpr : body.findAll(NameExpr.class)) {
      if (!assignmentTargets.contains(nameExpr)) {
        readRelation(nameExpr).ifPresent(relations::add);
      }
    }
    for (var fieldAccess : body.findAll(FieldAccessExpr.class)) {
      if (!assignmentTargets.contains(fieldAccess)) {
        readRelation(fieldAccess).ifPresent(relations::add);
      }
    }
    for (var throwStmt : body.findAll(ThrowStmt.class)) {
      throwsRelation(throwStmt).ifPresent(relations::add);
    }
    for (var catchClause : body.findAll(CatchClause.class)) {
      relations.addAll(catchesRelations(catchClause));
    }

    return relations;
  }

  private Optional<CiirRelation> typeReferenceRelation(
      CiirRelationKind kind, ClassOrInterfaceType typeRef) {
    try {
      var resolvedType = typeRef.resolve();
      var declaration = referenceTypeDeclarationOf(resolvedType);
      if (declaration.isEmpty()) {
        return Optional.empty();
      }
      var type = declaration.get();
      return Optional.of(
          resolvedRelation(
              kind,
              type,
              SymbolNaming.qualifiedName(type),
              SymbolNaming.canonicalName(type),
              CiirKind.TYPE,
              SourceLocationFactory.rangeOf(typeRef)));
    } catch (RuntimeException e) {
      return Optional.of(
          unresolvedRelation(
              kind,
              typeRef.getNameAsString(),
              e.getMessage(),
              SourceLocationFactory.rangeOf(typeRef)));
    }
  }

  private CiirRelation callRelation(MethodCallExpr call) {
    try {
      var resolved = call.resolve();
      return resolvedRelation(
          CiirRelationKind.CALLS,
          resolved,
          SymbolNaming.qualifiedName(resolved),
          SymbolNaming.canonicalName(resolved),
          CiirKind.METHOD,
          SourceLocationFactory.rangeOf(call));
    } catch (RuntimeException e) {
      return unresolvedRelation(
          CiirRelationKind.CALLS,
          call.getNameAsString() + "(...)",
          e.getMessage(),
          SourceLocationFactory.rangeOf(call));
    }
  }

  private CiirRelation constructsRelation(ObjectCreationExpr creation) {
    try {
      var resolvedConstructor = creation.resolve();
      var type = resolvedConstructor.declaringType();
      return resolvedRelation(
          CiirRelationKind.CONSTRUCTS,
          type,
          SymbolNaming.qualifiedName(type),
          SymbolNaming.canonicalName(type),
          CiirKind.TYPE,
          SourceLocationFactory.rangeOf(creation));
    } catch (RuntimeException e) {
      return unresolvedRelation(
          CiirRelationKind.CONSTRUCTS,
          creation.getTypeAsString(),
          e.getMessage(),
          SourceLocationFactory.rangeOf(creation));
    }
  }

  private Optional<CiirRelation> writeRelation(Expression target) {
    return resolveFieldAccess(target)
        .map(
            field ->
                resolvedRelation(
                    CiirRelationKind.WRITES,
                    field,
                    SymbolNaming.qualifiedName(field),
                    SymbolNaming.canonicalName(field),
                    CiirKind.FIELD,
                    SourceLocationFactory.rangeOf(target)));
  }

  private Optional<CiirRelation> readRelation(Expression expression) {
    return resolveFieldAccess(expression)
        .map(
            field ->
                resolvedRelation(
                    CiirRelationKind.READS,
                    field,
                    SymbolNaming.qualifiedName(field),
                    SymbolNaming.canonicalName(field),
                    CiirKind.FIELD,
                    SourceLocationFactory.rangeOf(expression)));
  }

  private static Optional<ResolvedFieldDeclaration> resolveFieldAccess(Expression expression) {
    try {
      ResolvedValueDeclaration resolved =
          switch (expression) {
            case NameExpr nameExpr -> nameExpr.resolve();
            case FieldAccessExpr fieldAccessExpr -> fieldAccessExpr.resolve();
            default -> null;
          };
      return resolved != null && resolved.isField()
          ? Optional.of(resolved.asField())
          : Optional.empty();
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  private Optional<CiirRelation> throwsRelation(ThrowStmt throwStmt) {
    try {
      var resolvedType = throwStmt.getExpression().calculateResolvedType();
      var declaration = referenceTypeDeclarationOf(resolvedType);
      if (declaration.isEmpty()) {
        return Optional.empty();
      }
      var type = declaration.get();
      return Optional.of(
          resolvedRelation(
              CiirRelationKind.THROWS,
              type,
              SymbolNaming.qualifiedName(type),
              SymbolNaming.canonicalName(type),
              CiirKind.TYPE,
              SourceLocationFactory.rangeOf(throwStmt)));
    } catch (RuntimeException e) {
      return Optional.of(
          unresolvedRelation(
              CiirRelationKind.THROWS,
              throwStmt.getExpression().toString(),
              e.getMessage(),
              SourceLocationFactory.rangeOf(throwStmt)));
    }
  }

  private List<CiirRelation> catchesRelations(CatchClause catchClause) {
    var relations = new ArrayList<CiirRelation>();
    for (var caughtType : flattenCaughtTypes(catchClause.getParameter().getType())) {
      try {
        var resolvedType = caughtType.resolve();
        var declaration = referenceTypeDeclarationOf(resolvedType);
        if (declaration.isPresent()) {
          var type = declaration.get();
          relations.add(
              resolvedRelation(
                  CiirRelationKind.CATCHES,
                  type,
                  SymbolNaming.qualifiedName(type),
                  SymbolNaming.canonicalName(type),
                  CiirKind.TYPE,
                  SourceLocationFactory.rangeOf(caughtType)));
        }
      } catch (RuntimeException e) {
        relations.add(
            unresolvedRelation(
                CiirRelationKind.CATCHES,
                caughtType.asString(),
                e.getMessage(),
                SourceLocationFactory.rangeOf(caughtType)));
      }
    }
    return relations;
  }

  private static List<ClassOrInterfaceType> flattenCaughtTypes(Type type) {
    if (type instanceof UnionType unionType) {
      return unionType.getElements().stream()
          .filter(ClassOrInterfaceType.class::isInstance)
          .map(ClassOrInterfaceType.class::cast)
          .toList();
    }
    if (type instanceof ClassOrInterfaceType classOrInterfaceType) {
      return List.of(classOrInterfaceType);
    }
    return List.of();
  }

  private static Optional<ResolvedReferenceTypeDeclaration> referenceTypeDeclarationOf(
      ResolvedType resolvedType) {
    return resolvedType.isReferenceType()
        ? resolvedType.asReferenceType().getTypeDeclaration()
        : Optional.empty();
  }

  private static boolean isSameSignature(ResolvedMethodDeclaration a, ResolvedMethodDeclaration b) {
    if (!a.getName().equals(b.getName()) || a.getNumberOfParams() != b.getNumberOfParams()) {
      return false;
    }
    for (int i = 0; i < a.getNumberOfParams(); i++) {
      if (!a.getParam(i).describeType().equals(b.getParam(i).describeType())) {
        return false;
      }
    }
    return true;
  }

  private CiirRelation resolvedRelation(
      CiirRelationKind kind,
      AssociableToAST resolvedDeclaration,
      String qualifiedSymbol,
      String canonicalSymbol,
      CiirKind targetKind,
      CiirRange location) {
    var classification = classifier.classify(resolvedDeclaration);
    String id = null;
    if (classification.status() == CiirResolutionStatus.RESOLVED
        && classification.owningProjectName() != null) {
      id =
          CiirIdentity.computeId(
              "java", classification.owningProjectName(), targetKind, canonicalSymbol);
    }
    return new CiirRelation(
        kind,
        new CiirRelationTarget(id, qualifiedSymbol),
        new CiirRelationResolution(classification.status(), classification.origin(), null),
        location);
  }

  private static CiirRelation unresolvedRelation(
      CiirRelationKind kind, String rawSymbolText, String reason, CiirRange location) {
    return new CiirRelation(
        kind,
        CiirRelationTarget.of(rawSymbolText),
        new CiirRelationResolution(
            CiirResolutionStatus.UNRESOLVED, CiirResolutionOrigin.UNKNOWN, reason),
        location);
  }
}
