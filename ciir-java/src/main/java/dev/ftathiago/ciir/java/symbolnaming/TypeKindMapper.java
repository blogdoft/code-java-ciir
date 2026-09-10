package dev.ftathiago.ciir.java.symbolnaming;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import dev.ftathiago.ciir.core.symbols.CiirTypeKind;

public final class TypeKindMapper {

  private TypeKindMapper() {}

  public static CiirTypeKind map(TypeDeclaration<?> declaration) {
    if (declaration instanceof ClassOrInterfaceDeclaration classOrInterface) {
      return classOrInterface.isInterface() ? CiirTypeKind.INTERFACE : CiirTypeKind.CLASS;
    }
    if (declaration instanceof EnumDeclaration) {
      return CiirTypeKind.ENUM;
    }
    if (declaration instanceof RecordDeclaration) {
      return CiirTypeKind.RECORD;
    }
    if (declaration instanceof AnnotationDeclaration) {
      return CiirTypeKind.ANNOTATION;
    }
    return CiirTypeKind.UNKNOWN;
  }
}
