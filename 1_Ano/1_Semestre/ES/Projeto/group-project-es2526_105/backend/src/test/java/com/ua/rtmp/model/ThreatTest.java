package com.ua.rtmp.model;

import com.ua.rtmp.model.enums.StrideCategory;
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

import static com.ua.rtmp.util.ThreatTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Threat Entity Tests")
class ThreatTest {

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
        @DisplayName("Given valid data, when creating threat with setters, should succeed")
        void givenValidData_whenCreatingThreatWithSetters_shouldSucceed() {
            // Given
            Threat threat = aSqlInjectionThreat().build();

            // When
            Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

            // Then
            assertThat(violations).isEmpty();
            assertThat(threat.getName()).isEqualTo("SQL Injection");
            assertThat(threat.getDescription()).isEqualTo("An attacker can inject malicious SQL statements");
            assertThat(threat.getCategory()).isEqualTo(StrideCategory.TAMPERING);
        }

        @Test
        @DisplayName("Given valid data, when creating threat with all-args constructor, should succeed")
        void givenValidData_whenCreatingThreatWithAllArgsConstructor_shouldSucceed() {
            // Given
            UUID expectedId = UUID.randomUUID();

            // When
            Threat threat = anXssThreat()
                    .withId(expectedId)
                    .buildWithConstructor();

            // Then
            assertThat(threat.getId()).isEqualTo(expectedId);
            assertThat(threat.getName()).isEqualTo("Cross-Site Scripting");
            assertThat(threat.getDescription()).isEqualTo("Malicious scripts executed in user browser");
            assertThat(threat.getCategory()).isEqualTo(StrideCategory.SPOOFING);
        }

        @Test
        @DisplayName("When creating threat with no-args constructor, should have null fields")
        void whenCreatingThreatWithNoArgsConstructor_shouldHaveNullFields() {
            // When
            Threat threat = new Threat();

            // Then
            assertThat(threat.getId()).isNull();
            assertThat(threat.getName()).isNull();
            assertThat(threat.getDescription()).isNull();
            assertThat(threat.getCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Nested
        @DisplayName("Name Validation")
        class NameValidation {

            @Test
            @DisplayName("Given blank name, when validating threat, should fail with required message")
            void givenBlankName_whenValidatingThreat_shouldFailWithRequiredMessage() {
                // Given
                Threat threat = aThreat().withBlankName().build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name is required");
            }

            @Test
            @DisplayName("Given null name, when validating threat, should fail with required message")
            void givenNullName_whenValidatingThreat_shouldFailWithRequiredMessage() {
                // Given
                Threat threat = aThreat().withNullName().build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name is required");
            }

            @Test
            @DisplayName("Given name exceeding 255 characters, when validating threat, should fail with size message")
            void givenNameExceeding255Characters_whenValidatingThreat_shouldFailWithSizeMessage() {
                // Given
                Threat threat = aThreat().withTooLongName().build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations)
                        .hasSize(1)
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name must not exceed 255 characters");
            }

            @Test
            @DisplayName("Given name with exactly 255 characters, when validating threat, should succeed")
            void givenNameWithExactly255Characters_whenValidatingThreat_shouldSucceed() {
                // Given
                Threat threat = aThreat().withMaxLengthName().build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations).isEmpty();
            }
        }

        @Nested
        @DisplayName("Optional Field Validation")
        class OptionalFieldValidation {

            @Test
            @DisplayName("Given null description, when validating threat, should succeed")
            void givenNullDescription_whenValidatingThreat_shouldSucceed() {
                // Given
                Threat threat = aThreat().withNullDescription().build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations).isEmpty();
            }

            @Test
            @DisplayName("Given null category, when validating threat, should succeed")
            void givenNullCategory_whenValidatingThreat_shouldSucceed() {
                // Given
                Threat threat = aThreat().withNullCategory().build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations).isEmpty();
            }
        }

        @Test
        @DisplayName("Given all STRIDE categories, when validating threats, should succeed for all")
        void givenAllStrideCategories_whenValidatingThreats_shouldSucceedForAll() {
            for (StrideCategory category : StrideCategory.values()) {
                // Given
                Threat threat = aThreat()
                        .withName("Test Threat " + category.name())
                        .withCategory(category)
                        .build();

                // When
                Set<ConstraintViolation<Threat>> violations = validator.validate(threat);

                // Then
                assertThat(violations)
                        .as("Validation should succeed for category: %s", category)
                        .isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Object Contract Tests")
    class ObjectContractTests {

        @Test
        @DisplayName("Given threats with same data, when comparing, should be equal with same hashCode")
        void givenThreatsWithSameData_whenComparing_shouldBeEqualWithSameHashCode() {
            // Given
            UUID sharedId = UUID.randomUUID();
            Threat threat1 = aSqlInjectionThreat()
                    .withId(sharedId)
                    .buildWithConstructor();
            Threat threat2 = aSqlInjectionThreat()
                    .withId(sharedId)
                    .buildWithConstructor();

            // Then
            assertThat(threat1)
                    .isEqualTo(threat2)
                    .hasSameHashCodeAs(threat2);
        }

        @Test
        @DisplayName("Given threats with different data, when comparing, should not be equal")
        void givenThreatsWithDifferentData_whenComparing_shouldNotBeEqual() {
            // Given
            Threat sqlInjectionThreat = aSqlInjectionThreat().withRandomId().build();
            Threat xssThreat = anXssThreat().withRandomId().build();

            // Then
            assertThat(sqlInjectionThreat).isNotEqualTo(xssThreat);
        }

        @Test
        @DisplayName("Given threat instance, when calling toString, should contain all field values")
        void givenThreatInstance_whenCallingToString_shouldContainAllFieldValues() {
            // Given
            UUID threatId = UUID.randomUUID();
            Threat threat = aSqlInjectionThreat()
                    .withId(threatId)
                    .withDescription("Test Description")
                    .buildWithConstructor();

            // When
            String toString = threat.toString();

            // Then
            assertThat(toString)
                    .contains("SQL Injection")
                    .contains("Test Description")
                    .contains("TAMPERING")
                    .contains(threatId.toString());
        }

        @Test
        @DisplayName("Given threat with null fields, when calling toString, should handle nulls gracefully")
        void givenThreatWithNullFields_whenCallingToString_shouldHandleNullsGracefully() {
            // Given
            Threat threat = aThreat()
                    .withNullDescription()
                    .withNullCategory()
                    .build();

            // When
            String toString = threat.toString();

            // Then
            assertThat(toString).isNotNull();
        }
    }
}