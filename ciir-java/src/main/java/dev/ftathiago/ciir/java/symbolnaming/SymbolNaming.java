package dev.ftathiago.ciir.java.symbolnaming;

import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Builds {@code qualifiedName}/{@code canonicalName}/{@code container} from JavaParser's
 * <em>resolved</em> declarations — the same code path is used both for the entity CIIR document
 * being emitted (calling {@code .resolve()} on its own declaration node) and for a relation's
 * target symbol, so the two always agree byte-for-byte.
 *
 * <p>Only types and members (methods/constructors/fields) are overloadable/nameable in a way that
 * matters here; local variables and parameters never get their own CIIR document, so this class has
 * no method for them.
 */
public final class SymbolNaming {

  private SymbolNaming() {}

  /**
   * Fully qualified, dotted name — types have no parameter list, so this is also the canonical
   * name.
   */
  public static String qualifiedName(ResolvedTypeDeclaration type) {
    return type.getQualifiedName();
  }

  public static String canonicalName(ResolvedTypeDeclaration type) {
    return qualifiedName(type);
  }

  public static String container(ResolvedTypeDeclaration type) {
    var containerType = type.containerType();
    if (containerType.isPresent()) {
      return containerType.get().getQualifiedName();
    }
    var packageName = type.getPackageName();
    return packageName.isEmpty() ? null : packageName;
  }

  /**
   * {@code Container.methodName} — no parameter list; only {@link #canonicalName} distinguishes
   * overloads.
   */
  public static String qualifiedName(ResolvedMethodLikeDeclaration method) {
    return method.getQualifiedName();
  }

  public static String canonicalName(ResolvedMethodLikeDeclaration method) {
    return qualifiedName(method) + "(" + parameterTypeList(method) + ")";
  }

  public static String container(ResolvedMethodLikeDeclaration method) {
    return method.declaringType().getQualifiedName();
  }

  /**
   * A constructor's readable name is its declaring type's simple/qualified name, not {@code
   * <init>}.
   */
  public static String qualifiedName(ResolvedConstructorDeclaration constructor) {
    return constructor.declaringType().getQualifiedName();
  }

  public static String canonicalName(ResolvedConstructorDeclaration constructor) {
    return qualifiedName(constructor) + "(" + parameterTypeList(constructor) + ")";
  }

  public static String container(ResolvedConstructorDeclaration constructor) {
    return constructor.declaringType().getQualifiedName();
  }

  public static String qualifiedName(ResolvedFieldDeclaration field) {
    return field.declaringType().getQualifiedName() + "." + field.getName();
  }

  public static String canonicalName(ResolvedFieldDeclaration field) {
    return qualifiedName(field);
  }

  public static String container(ResolvedFieldDeclaration field) {
    return field.declaringType().getQualifiedName();
  }

  /** Fully qualified name of a resolved method's return type, e.g. {@code java.lang.String}. */
  public static String returnType(ResolvedMethodDeclaration method) {
    return method.getReturnType().describe();
  }

  private static String parameterTypeList(ResolvedMethodLikeDeclaration method) {
    return IntStream.range(0, method.getNumberOfParams())
        .mapToObj(i -> method.getParam(i).describeType())
        .collect(Collectors.joining(","));
  }
}
