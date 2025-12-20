package com.ua.rtmp.model;

import com.ua.rtmp.util.ComponentTestDataBuilder;
import com.ua.rtmp.util.ThreatModelTestDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Component Entity Tests")
class ComponentTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Given valid data, when creating component with setters, should succeed")
        void givenValidData_whenCreatingComponentWithSetters_shouldSucceed() {
            Component component = ComponentTestDataBuilder.aWebServerComponent().build();

            Set<ConstraintViolation<Component>> violations = validator.validate(component);

            assertThat(violations).isEmpty();
            assertThat(component.getName()).isEqualTo("Web Server");
            assertThat(component.getDescription()).isEqualTo("Handles HTTP requests and serves web content");
        }

        @Test
        @DisplayName("Given valid data, when creating component with all-args constructor, should succeed")
        void givenValidData_whenCreatingComponentWithAllArgsConstructor_shouldSucceed() {
            UUID expectedId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();

            Component component = ComponentTestDataBuilder.aDatabaseComponent()
                    .withId(expectedId)
                    .withThreatModel(threatModel)
                    .buildWithConstructor();

            assertThat(component.getId()).isEqualTo(expectedId);
            assertThat(component.getName()).isEqualTo("Database Server");
            assertThat(component.getDescription()).isEqualTo("Stores and manages application data");
            assertThat(component.getThreatModel()).isEqualTo(threatModel);
        }

        @Test
        @DisplayName("When creating component with no-args constructor, should have null fields")
        void whenCreatingComponentWithNoArgsConstructor_shouldHaveNullFields() {
            Component component = new Component();

            assertThat(component.getId()).isNull();
            assertThat(component.getName()).isNull();
            assertThat(component.getDescription()).isNull();
            assertThat(component.getThreatModel()).isNull();
            assertThat(component.getVulnerabilities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Nested
        @DisplayName("Name Validation")
        class NameValidation {

            @Test
            @DisplayName("Given blank name, when validating component, should fail with required message")
            void givenBlankName_whenValidatingComponent_shouldFailWithRequiredMessage() {
                Component component = ComponentTestDataBuilder.aComponent().withBlankName().build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name is required");
            }

            @Test
            @DisplayName("Given null name, when validating component, should fail with required message")
            void givenNullName_whenValidatingComponent_shouldFailWithRequiredMessage() {
                Component component = ComponentTestDataBuilder.aComponent().withNullName().build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name is required");
            }

            @Test
            @DisplayName("Given name with max length, when validating component, should pass")
            void givenNameWithMaxLength_whenValidatingComponent_shouldPass() {
                Component component = ComponentTestDataBuilder.aComponent().withMaxLengthName().build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations).isEmpty();
                assertThat(component.getName()).hasSize(255);
            }

            @Test
            @DisplayName("Given name exceeding max length, when validating component, should fail")
            void givenNameExceedingMaxLength_whenValidatingComponent_shouldFail() {
                Component component = ComponentTestDataBuilder.aComponent().withTooLongName().build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name must not exceed 255 characters");
            }
        }

        @Nested
        @DisplayName("Optional Field Validation")
        class OptionalFieldValidation {

            @Test
            @DisplayName("Given null description, when validating component, should pass")
            void givenNullDescription_whenValidatingComponent_shouldPass() {
                Component component = ComponentTestDataBuilder.aComponent()
                        .withName("Valid Component")
                        .withNullDescription()
                        .build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations).isEmpty();
            }

            @Test
            @DisplayName("Given empty description, when validating component, should pass")
            void givenEmptyDescription_whenValidatingComponent_shouldPass() {
                Component component = ComponentTestDataBuilder.aComponent()
                        .withName("Valid Component")
                        .withDescription("")
                        .build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations).isEmpty();
            }

            @Test
            @DisplayName("Given long description, when validating component, should pass")
            void givenLongDescription_whenValidatingComponent_shouldPass() {
                String longDescription = "A".repeat(5000);
                Component component = ComponentTestDataBuilder.aComponent()
                        .withName("Valid Component")
                        .withDescription(longDescription)
                        .build();

                Set<ConstraintViolation<Component>> violations = validator.validate(component);

                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Object Contract Tests")
    class ObjectContractTests {

        @Test
        @DisplayName("Given same component data, when comparing with equals, should be equal")
        void givenSameComponentData_whenComparingWithEquals_shouldBeEqual() {
            UUID id = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();

            Component component1 = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(id)
                    .withThreatModel(threatModel)
                    .build();
            Component component2 = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(id)
                    .withThreatModel(threatModel)
                    .build();

            assertThat(component1).isEqualTo(component2);
        }

        @Test
        @DisplayName("Given different component data, when comparing with equals, should not be equal")
        void givenDifferentComponentData_whenComparingWithEquals_shouldNotBeEqual() {
            Component component1 = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(UUID.randomUUID())
                    .build();
            Component component2 = ComponentTestDataBuilder.aDatabaseComponent()
                    .withId(UUID.randomUUID())
                    .build();

            assertThat(component1).isNotEqualTo(component2);
        }

        @Test
        @DisplayName("Given same component data, when comparing hashCode, should be equal")
        void givenSameComponentData_whenComparingHashCode_shouldBeEqual() {
            UUID id = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();

            Component component1 = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(id)
                    .withThreatModel(threatModel)
                    .build();
            Component component2 = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(id)
                    .withThreatModel(threatModel)
                    .build();

            assertThat(component1.hashCode()).isEqualTo(component2.hashCode());
        }

        @Test
        @DisplayName("Given component, when calling toString, should contain key fields")
        void givenComponent_whenCallingToString_shouldContainKeyFields() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(UUID.randomUUID())
                    .build();

            String result = component.toString();

            assertThat(result).contains("Web Server");
            assertThat(result).contains("Handles HTTP requests");
        }
    }

    @Nested
    @DisplayName("Vulnerability Relationship Tests")
    class VulnerabilityRelationshipTests {

        @Test
        @DisplayName("Given new component, when adding vulnerability, should establish bidirectional relationship")
        void givenNewComponent_whenAddingVulnerability_shouldEstablishBidirectionalRelationship() {
            Component component = ComponentTestDataBuilder.aWebServerComponent().build();
            Vulnerability vulnerability = new Vulnerability();
            vulnerability.setLikelihood(3);
            vulnerability.setImpact(4);

            component.addVulnerability(vulnerability);

            assertThat(component.getVulnerabilities()).hasSize(1);
            assertThat(component.getVulnerabilities()).contains(vulnerability);
            assertThat(vulnerability.getComponent()).isEqualTo(component);
        }

        @Test
        @DisplayName("Given component with vulnerability, when removing vulnerability, should break bidirectional relationship")
        void givenComponentWithVulnerability_whenRemovingVulnerability_shouldBreakBidirectionalRelationship() {
            Component component = ComponentTestDataBuilder.aWebServerComponent().build();
            Vulnerability vulnerability = new Vulnerability();
            vulnerability.setLikelihood(3);
            vulnerability.setImpact(4);
            component.addVulnerability(vulnerability);

            component.removeVulnerability(vulnerability);

            assertThat(component.getVulnerabilities()).isEmpty();
            assertThat(vulnerability.getComponent()).isNull();
        }

        @Test
        @DisplayName("Given component, when adding multiple vulnerabilities, should maintain all relationships")
        void givenComponent_whenAddingMultipleVulnerabilities_shouldMaintainAllRelationships() {
            Component component = ComponentTestDataBuilder.aWebServerComponent().build();
            Vulnerability vuln1 = new Vulnerability();
            vuln1.setLikelihood(3);
            vuln1.setImpact(4);
            Vulnerability vuln2 = new Vulnerability();
            vuln2.setLikelihood(2);
            vuln2.setImpact(5);
            Vulnerability vuln3 = new Vulnerability();
            vuln3.setLikelihood(4);
            vuln3.setImpact(3);

            component.addVulnerability(vuln1);
            component.addVulnerability(vuln2);
            component.addVulnerability(vuln3);

            assertThat(component.getVulnerabilities()).hasSize(3);
            assertThat(component.getVulnerabilities()).containsExactly(vuln1, vuln2, vuln3);
            assertThat(vuln1.getComponent()).isEqualTo(component);
            assertThat(vuln2.getComponent()).isEqualTo(component);
            assertThat(vuln3.getComponent()).isEqualTo(component);
        }

        @Test
        @DisplayName("Given component with vulnerabilities, when initializing with constructor, should set list")
        void givenComponentWithVulnerabilities_whenInitializingWithConstructor_shouldSetList() {
            Vulnerability vuln1 = new Vulnerability();
            vuln1.setLikelihood(3);
            vuln1.setImpact(4);
            Vulnerability vuln2 = new Vulnerability();
            vuln2.setLikelihood(2);
            vuln2.setImpact(5);

            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withVulnerabilities(java.util.List.of(vuln1, vuln2))
                    .build();

            assertThat(component.getVulnerabilities()).hasSize(2);
            assertThat(component.getVulnerabilities()).containsExactly(vuln1, vuln2);
        }
    }
}
