package dev.ftathiago.ciir.application.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * {@code ciir-core} and {@code ciir-application} must never depend on JavaParser — only {@code
 * ciir-java} may. This fails the build the moment such a dependency (and a violating import) is
 * introduced, the same guarantee the C# generator enforces via a reflection-based "no referenced
 * Microsoft.CodeAnalysis assembly" test.
 */
class ArchitectureBoundaryTest {

  @Test
  void coreAndApplicationNeverDependOnJavaParser() {
    // Surefire launches its forked JVM through a booter jar whose manifest Class-Path
    // shortcut hides the real classpath from `java.class.path`, so ArchUnit's default
    // classpath auto-detection (importPackages/importClasspath) finds nothing here. Point it
    // directly at each sibling module's compiled output instead.
    var moduleDir = Path.of(System.getProperty("user.dir"));
    JavaClasses importedClasses =
        new ClassFileImporter()
            .importPaths(
                moduleDir.resolve("target/classes"),
                moduleDir.resolve("../ciir-core/target/classes"));
    assertThat(importedClasses).isNotEmpty();

    noClasses()
        .that()
        .resideInAnyPackage("dev.ftathiago.ciir.core..", "dev.ftathiago.ciir.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.github.javaparser..")
        .check(importedClasses);
  }
}
