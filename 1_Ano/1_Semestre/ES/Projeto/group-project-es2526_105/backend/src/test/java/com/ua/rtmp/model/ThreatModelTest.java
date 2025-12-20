package com.ua.rtmp.model;

import com.ua.rtmp.util.ThreatModelTestDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThreatModel Entity Tests")
class ThreatModelTest {

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
        @DisplayName("Given valid data, when creating threat model with setters, should succeed")
        void givenValidData_whenCreatingThreatModelWithSetters_shouldSucceed() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();

            Set<ConstraintViolation<ThreatModel>> violations = validator.validate(threatModel);

            assertThat(violations).isEmpty();
            assertThat(threatModel.getName()).isEqualTo("Payment System Threat Model");
            assertThat(threatModel.getDescription()).isEqualTo("Covers threats and mitigations for the payment processing system");
        }

        @Test
        @DisplayName("Given valid data, when creating threat model with all-args constructor, should succeed")
        void givenValidData_whenCreatingThreatModelWithAllArgsConstructor_shouldSucceed() {
            UUID expectedId = UUID.randomUUID();
            LocalDateTime expectedTime = LocalDateTime.now();

            ThreatModel threatModel = ThreatModelTestDataBuilder.aWebApplicationThreatModel()
                    .withId(expectedId)
                    .withCreatedAt(expectedTime)
                    .buildWithConstructor();

            assertThat(threatModel.getId()).isEqualTo(expectedId);
            assertThat(threatModel.getName()).isEqualTo("Web Application Threat Model");
            assertThat(threatModel.getDescription()).isEqualTo("Security threats for the web application layer");
            assertThat(threatModel.getCreatedAt()).isEqualTo(expectedTime);
        }

        @Test
        @DisplayName("When creating threat model with no-args constructor, should have null fields")
        void whenCreatingThreatModelWithNoArgsConstructor_shouldHaveNullFields() {
            ThreatModel threatModel = new ThreatModel();

            assertThat(threatModel.getId()).isNull();
            assertThat(threatModel.getName()).isNull();
            assertThat(threatModel.getDescription()).isNull();
            assertThat(threatModel.getCreatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Nested
        @DisplayName("Name Validation")
        class NameValidation {

            @Test
            @DisplayName("Given blank name, when validating threat model, should fail with required message")
            void givenBlankName_whenValidatingThreatModel_shouldFailWithRequiredMessage() {
                ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().withBlankName().build();

                Set<ConstraintViolation<ThreatModel>> violations = validator.validate(threatModel);

                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name is required");
            }

            @Test
            @DisplayName("Given null name, when validating threat model, should fail with required message")
            void givenNullName_whenValidatingThreatModel_shouldFailWithRequiredMessage() {
                ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().withNullName().build();

                Set<ConstraintViolation<ThreatModel>> violations = validator.validate(threatModel);

                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name is required");
            }

            @Test
            @DisplayName("Given name exceeding 255 characters, when validating threat model, should fail with size message")
            void givenNameExceeding255Characters_whenValidatingThreatModel_shouldFailWithSizeMessage() {
                ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().withTooLongName().build();

                Set<ConstraintViolation<ThreatModel>> violations = validator.validate(threatModel);

                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name must not exceed 255 characters");
            }

            @Test
            @DisplayName("Given name with exactly 255 characters, when validating threat model, should succeed")
            void givenNameWithExactly255Characters_whenValidatingThreatModel_shouldSucceed() {
                ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().withMaxLengthName().build();

                Set<ConstraintViolation<ThreatModel>> violations = validator.validate(threatModel);

                assertThat(violations).isEmpty();
            }
        }

        @Nested
        @DisplayName("Optional Field Validation")
        class OptionalFieldValidation {

            @Test
            @DisplayName("Given null description, when validating threat model, should succeed")
            void givenNullDescription_whenValidatingThreatModel_shouldSucceed() {
                ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().withNullDescription().build();

                Set<ConstraintViolation<ThreatModel>> violations = validator.validate(threatModel);

                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Object Contract Tests")
    class ObjectContractTests {

        @Test
        @DisplayName("Given threat models with same data, when comparing, should be equal with same hashCode")
        void givenThreatModelsWithSameData_whenComparing_shouldBeEqualWithSameHashCode() {
            UUID sharedId = UUID.randomUUID();
            ThreatModel threatModel1 = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(sharedId)
                    .buildWithConstructor();
            ThreatModel threatModel2 = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(sharedId)
                    .buildWithConstructor();

            assertThat(threatModel1)
                    .isEqualTo(threatModel2)
                    .hasSameHashCodeAs(threatModel2);
        }

        @Test
        @DisplayName("Given threat models with different data, when comparing, should not be equal")
        void givenThreatModelsWithDifferentData_whenComparing_shouldNotBeEqual() {
            ThreatModel paymentModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withRandomId().build();
            ThreatModel webAppModel = ThreatModelTestDataBuilder.aWebApplicationThreatModel().withRandomId().build();

            assertThat(paymentModel).isNotEqualTo(webAppModel);
        }

        @Test
        @DisplayName("Given threat model instance, when calling toString, should contain all field values")
        void givenThreatModelInstance_whenCallingToString_shouldContainAllFieldValues() {
            UUID modelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(modelId)
                    .withDescription("Test Description")
                    .buildWithConstructor();

            String toString = threatModel.toString();

            assertThat(toString)
                    .contains("Payment System Threat Model")
                    .contains("Test Description")
                    .contains(modelId.toString());
        }

        @Test
        @DisplayName("Given threat model with null fields, when calling toString, should handle nulls gracefully")
        void givenThreatModelWithNullFields_whenCallingToString_shouldHandleNullsGracefully() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel()
                    .withNullDescription()
                    .build();

            String toString = threatModel.toString();

            assertThat(toString).isNotNull();
        }
    }

    @Nested
    @DisplayName("Component Relationship Tests")
    class ComponentRelationshipTests {

        @Test
        @DisplayName("Given new threat model, when accessing components, should have empty list")
        void givenNewThreatModel_whenAccessingComponents_shouldHaveEmptyList() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().build();

            assertThat(threatModel.getComponents()).isEmpty();
        }

        @Test
        @DisplayName("Given threat model and component, when adding component, should establish bidirectional relationship")
        void givenThreatModelAndComponent_whenAddingComponent_shouldEstablishBidirectionalRelationship() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().build();
            Component component = new Component();
            component.setName("Test Component");

            threatModel.addComponent(component);

            assertThat(threatModel.getComponents()).contains(component);
            assertThat(component.getThreatModel()).isEqualTo(threatModel);
        }

        @Test
        @DisplayName("Given threat model with component, when removing component, should break bidirectional relationship")
        void givenThreatModelWithComponent_whenRemovingComponent_shouldBreakBidirectionalRelationship() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel().build();
            Component component = new Component();
            component.setName("Test Component");
            threatModel.addComponent(component);

            threatModel.removeComponent(component);

            assertThat(threatModel.getComponents()).doesNotContain(component);
            assertThat(component.getThreatModel()).isNull();
        }
    }
}
