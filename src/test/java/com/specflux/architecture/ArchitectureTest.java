package com.specflux.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture tests to enforce coding standards and patterns.
 *
 * <p>These tests ensure that the codebase follows API-first development practices, where
 * controllers implement interfaces generated from the OpenAPI specification.
 */
class ArchitectureTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setUp() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.specflux");
  }

  @Test
  @DisplayName("REST controllers should implement generated API interfaces")
  void restControllersShouldImplementGeneratedApiInterfaces() {
    DescribedPredicate<JavaClass> isGeneratedApiInterface =
        new DescribedPredicate<>("a generated API interface from com.specflux.api.generated") {
          @Override
          public boolean test(JavaClass javaClass) {
            return javaClass.getPackageName().equals("com.specflux.api.generated")
                && javaClass.getSimpleName().endsWith("Api");
          }
        };

    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .and()
            .resideInAPackage("..interfaces.rest..")
            .should()
            .implement(isGeneratedApiInterface)
            .because(
                "REST controllers must implement the generated API interface from OpenAPI spec "
                    + "to ensure API-first development. The interface is generated in "
                    + "com.specflux.api.generated package with names ending in 'Api'.");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Controllers should not define their own request mappings")
  void controllersShouldNotDefineOwnRequestMappings() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .and()
            .resideInAPackage("..interfaces.rest..")
            .should()
            .notBeAnnotatedWith(org.springframework.web.bind.annotation.RequestMapping.class)
            .because(
                "Controllers implementing generated API interfaces should not use @RequestMapping. "
                    + "The path mappings are defined in the OpenAPI spec and inherited from the interface.");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Generated API package should only contain generated code")
  void generatedApiPackageShouldOnlyContainGeneratedCode() {
    // This test ensures no hand-written code ends up in the generated package
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.specflux.api.generated..")
            .should()
            .haveSimpleNameEndingWith("Api")
            .orShould()
            .haveSimpleNameEndingWith("Dto")
            .orShould()
            .haveSimpleName("ApiUtil")
            .because(
                "The generated API package should only contain generated interfaces and DTOs.");

    rule.check(importedClasses);
  }
}
