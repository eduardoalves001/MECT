package com.ua.rtmp.repository;

import com.ua.rtmp.model.Threat;
import com.ua.rtmp.model.enums.StrideCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ua.rtmp.util.ThreatTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("Threat Repository Integration Tests")
class ThreatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ThreatRepository threatRepository;

    private Threat sqlInjectionThreat;
    private Threat xssThreat;

    @BeforeEach
    void setUp() {
        sqlInjectionThreat = aSqlInjectionThreat().build();
        xssThreat = anXssThreat().build();
    }

    @Test
    void save_WithValidThreat_ShouldPersistThreat() {
        // When
        Threat savedThreat = threatRepository.save(sqlInjectionThreat);

        // Then
        assertThat(savedThreat).isNotNull();
        assertThat(savedThreat.getId()).isNotNull();
        assertThat(savedThreat.getName()).isEqualTo("SQL Injection");
        assertThat(savedThreat.getDescription()).isEqualTo("An attacker can inject malicious SQL statements");
        assertThat(savedThreat.getCategory()).isEqualTo(StrideCategory.TAMPERING);
    }

    @Test
    void findAll_WithMultipleThreats_ShouldReturnAllThreats() {
        // Given
        entityManager.persistAndFlush(sqlInjectionThreat);
        entityManager.persistAndFlush(xssThreat);

        // When
        List<Threat> threats = threatRepository.findAll();

        // Then
        assertThat(threats).hasSize(2);
        assertThat(threats).extracting(Threat::getName)
                .containsExactlyInAnyOrder("SQL Injection", "Cross-Site Scripting");
    }

    @Test
    void findAll_WithNoThreats_ShouldReturnEmptyList() {
        // When
        List<Threat> threats = threatRepository.findAll();

        // Then
        assertThat(threats).isEmpty();
    }

    @Test
    void findById_WithExistingThreat_ShouldReturnThreat() {
        // Given
        Threat persistedThreat = entityManager.persistAndFlush(sqlInjectionThreat);
        UUID threatId = persistedThreat.getId();

        // When
        Optional<Threat> foundThreat = threatRepository.findById(threatId);

        // Then
        assertThat(foundThreat).isPresent();
        assertThat(foundThreat.get().getName()).isEqualTo("SQL Injection");
        assertThat(foundThreat.get().getId()).isEqualTo(threatId);
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Threat> foundThreat = threatRepository.findById(nonExistentId);

        // Then
        assertThat(foundThreat).isEmpty();
    }

    @Test
    void existsByName_WithExistingName_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(sqlInjectionThreat);

        // When
        boolean exists = threatRepository.existsByName("SQL Injection");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_WithNonExistentName_ShouldReturnFalse() {
        // Given
        entityManager.persistAndFlush(sqlInjectionThreat);

        // When
        boolean exists = threatRepository.existsByName("Non-existent Threat");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByName_WithEmptyDatabase_ShouldReturnFalse() {
        // When
        boolean exists = threatRepository.existsByName("Any Name");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByName_WithNullName_ShouldReturnFalse() {
        // Given
        entityManager.persistAndFlush(sqlInjectionThreat);

        // When
        boolean exists = threatRepository.existsByName(null);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByName_IsCaseSensitive() {
        // Given
        entityManager.persistAndFlush(sqlInjectionThreat);

        // When
        boolean existsExact = threatRepository.existsByName("SQL Injection");
        boolean existsLower = threatRepository.existsByName("sql injection");
        boolean existsUpper = threatRepository.existsByName("SQL INJECTION");

        // Then
        assertThat(existsExact).isTrue();
        assertThat(existsLower).isFalse();
        assertThat(existsUpper).isFalse();
    }

    @Test
    void delete_WithExistingThreat_ShouldRemoveThreat() {
        // Given
        Threat persistedThreat = entityManager.persistAndFlush(sqlInjectionThreat);
        UUID threatId = persistedThreat.getId();

        // When
        threatRepository.delete(persistedThreat);
        entityManager.flush();

        // Then
        Optional<Threat> deletedThreat = threatRepository.findById(threatId);
        assertThat(deletedThreat).isEmpty();
    }

    @Test
    void count_WithMultipleThreats_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(sqlInjectionThreat);
        entityManager.persistAndFlush(xssThreat);

        // When
        long count = threatRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void save_WithNullDescription_ShouldPersistSuccessfully() {
        // Given
        sqlInjectionThreat.setDescription(null);

        // When
        Threat savedThreat = threatRepository.save(sqlInjectionThreat);

        // Then
        assertThat(savedThreat).isNotNull();
        assertThat(savedThreat.getDescription()).isNull();
        assertThat(savedThreat.getName()).isEqualTo("SQL Injection");
    }

    @Test
    void save_WithNullCategory_ShouldPersistSuccessfully() {
        // Given
        sqlInjectionThreat.setCategory(null);

        // When
        Threat savedThreat = threatRepository.save(sqlInjectionThreat);

        // Then
        assertThat(savedThreat).isNotNull();
        assertThat(savedThreat.getCategory()).isNull();
        assertThat(savedThreat.getName()).isEqualTo("SQL Injection");
    }

    @Test
    void save_WithAllStrideCategories_ShouldPersistSuccessfully() {
        for (StrideCategory category : StrideCategory.values()) {
            // Given
            Threat threat = new Threat();
            threat.setName("Test Threat " + category.name());
            threat.setDescription("Test description");
            threat.setCategory(category);

            // When
            Threat savedThreat = threatRepository.save(threat);

            // Then
            assertThat(savedThreat).isNotNull();
            assertThat(savedThreat.getCategory()).isEqualTo(category);

            // Clean up for next iteration
            threatRepository.delete(savedThreat);
        }
    }

    @Test
    void save_WithMaxLengthName_ShouldPersistSuccessfully() {
        // Given
        String maxLengthName = "A".repeat(255);
        sqlInjectionThreat.setName(maxLengthName);

        // When
        Threat savedThreat = threatRepository.save(sqlInjectionThreat);

        // Then
        assertThat(savedThreat).isNotNull();
        assertThat(savedThreat.getName()).isEqualTo(maxLengthName);
    }
}