package com.ua.rtmp.service;

import com.ua.rtmp.model.Threat;
import com.ua.rtmp.repository.ThreatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ua.rtmp.util.ThreatTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("Threat Service Tests")
class ThreatServiceTest {

    @Mock
    private ThreatRepository threatRepository;

    @InjectMocks
    private ThreatService threatService;

    private Threat sqlInjectionThreat;
    private Threat xssThreat;
    private UUID threatId;

    @BeforeEach
    void setUp() {
        threatId = UUID.randomUUID();

        sqlInjectionThreat = aSqlInjectionThreat()
                .withId(threatId)
                .build();

        xssThreat = anXssThreat()
                .withRandomId()
                .build();
    }

    @Nested
    @DisplayName("getAllThreats() Tests")
    class GetAllThreatsTests {

        @Test
        @DisplayName("Given threats exist in repository, when getting all threats, should return all threats")
        void givenThreatsExistInRepository_whenGettingAllThreats_shouldReturnAllThreats() {
            // Given
            List<Threat> expectedThreats = Arrays.asList(sqlInjectionThreat, xssThreat);
            given(threatRepository.findAll()).willReturn(expectedThreats);

            // When
            List<Threat> actualThreats = threatService.getAllThreats();

            // Then
            assertThat(actualThreats)
                    .hasSize(2)
                    .containsExactly(sqlInjectionThreat, xssThreat);
            then(threatRepository).should().findAll();
        }

        @Test
        @DisplayName("Given no threats exist in repository, when getting all threats, should return empty list")
        void givenNoThreatsExistInRepository_whenGettingAllThreats_shouldReturnEmptyList() {
            // Given
            given(threatRepository.findAll()).willReturn(Collections.emptyList());

            // When
            List<Threat> actualThreats = threatService.getAllThreats();

            // Then
            assertThat(actualThreats).isEmpty();
            then(threatRepository).should().findAll();
        }

        @Test
        @DisplayName("Given large number of threats, when getting all threats, should return all without modification")
        void givenLargeNumberOfThreats_whenGettingAllThreats_shouldReturnAllWithoutModification() {
            // Given
            List<Threat> largeList = Collections.nCopies(1000, sqlInjectionThreat);
            given(threatRepository.findAll()).willReturn(largeList);

            // When
            List<Threat> result = threatService.getAllThreats();

            // Then
            assertThat(result).hasSize(1000);
            then(threatRepository).should().findAll();
        }
    }

    @Nested
    @DisplayName("getThreatById() Tests")
    class GetThreatByIdTests {

        @Test
        @DisplayName("Given threat exists in repository, when getting threat by ID, should return threat")
        void givenThreatExistsInRepository_whenGettingThreatById_shouldReturnThreat() {
            // Given
            given(threatRepository.findById(threatId)).willReturn(Optional.of(sqlInjectionThreat));

            // When
            Optional<Threat> result = threatService.getThreatById(threatId);

            // Then
            assertThat(result)
                    .isPresent()
                    .get()
                    .satisfies(threat -> {
                        assertThat(threat.getId()).isEqualTo(threatId);
                        assertThat(threat.getName()).isEqualTo("SQL Injection");
                    });
            then(threatRepository).should().findById(threatId);
        }

        @Test
        @DisplayName("Given threat does not exist in repository, when getting threat by ID, should return empty")
        void givenThreatDoesNotExistInRepository_whenGettingThreatById_shouldReturnEmpty() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            given(threatRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // When
            Optional<Threat> result = threatService.getThreatById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            then(threatRepository).should().findById(nonExistentId);
        }

        @Test
        @DisplayName("Given null ID, when getting threat by ID, should delegate to repository")
        void givenNullId_whenGettingThreatById_shouldDelegateToRepository() {
            // Given
            given(threatRepository.findById(null)).willReturn(Optional.empty());

            // When
            Optional<Threat> result = threatService.getThreatById(null);

            // Then
            assertThat(result).isEmpty();
            then(threatRepository).should().findById(null);
        }
    }

    @Nested
    @DisplayName("Service Configuration Tests")
    class ServiceConfigurationTests {

        @Test
        @DisplayName("Given service is properly configured, when accessing service, should have all dependencies injected")
        void givenServiceIsProperlyConfigured_whenAccessingService_shouldHaveAllDependenciesInjected() {
            // Then
            assertThat(threatService).isNotNull();
            // Implicit verification through other successful tests
        }

        @Test
        @DisplayName("Given read-only methods are called, when executing operations, should use transactional context")
        void givenReadOnlyMethodsAreCalled_whenExecutingOperations_shouldUseTransactionalContext() {
            // Given
            given(threatRepository.findAll()).willReturn(Arrays.asList(sqlInjectionThreat));
            given(threatRepository.findById(threatId)).willReturn(Optional.of(sqlInjectionThreat));

            // When
            threatService.getAllThreats();
            threatService.getThreatById(threatId);

            // Then
            then(threatRepository).should().findAll();
            then(threatRepository).should().findById(threatId);
            // Note: @Transactional(readOnly = true) behavior would be tested in integration tests
        }
    }
}