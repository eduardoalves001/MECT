package com.ua.rtmp.repository;

import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.util.ThreatModelTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ThreatModel Repository Tests")
class ThreatModelRepositoryTest {

    @Autowired
    private ThreatModelRepository threatModelRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        threatModelRepository.deleteAll();
    }

    @Nested
    @DisplayName("Save and Retrieve Tests")
    class SaveAndRetrieveTests {

        @Test
        @DisplayName("Given valid threat model, when saving, should persist to database")
        void givenValidThreatModel_whenSaving_shouldPersistToDatabase() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();

            ThreatModel saved = threatModelRepository.save(threatModel);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Payment System Threat Model");
            assertThat(saved.getDescription()).isEqualTo("Covers threats and mitigations for the payment processing system");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Given saved threat model, when finding by id, should retrieve it")
        void givenSavedThreatModel_whenFindingById_shouldRetrieveIt() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            ThreatModel saved = threatModelRepository.save(threatModel);
            entityManager.flush();
            entityManager.clear();

            Optional<ThreatModel> found = threatModelRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Payment System Threat Model");
        }

        @Test
        @DisplayName("Given non-existent id, when finding by id, should return empty")
        void givenNonExistentId_whenFindingById_shouldReturnEmpty() {
            UUID nonExistentId = UUID.randomUUID();

            Optional<ThreatModel> found = threatModelRepository.findById(nonExistentId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Given multiple threat models, when finding all, should retrieve all")
        void givenMultipleThreatModels_whenFindingAll_shouldRetrieveAll() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.anApiThreatModel().build());

            List<ThreatModel> allModels = threatModelRepository.findAll();

            assertThat(allModels).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Exists By Name Tests")
    class ExistsByNameTests {

        @Test
        @DisplayName("Given saved threat model, when checking exists by name, should return true")
        void givenSavedThreatModel_whenCheckingExistsByName_shouldReturnTrue() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            threatModelRepository.save(threatModel);

            boolean exists = threatModelRepository.existsByName("Payment System Threat Model");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Given non-existent name, when checking exists by name, should return false")
        void givenNonExistentName_whenCheckingExistsByName_shouldReturnFalse() {
            boolean exists = threatModelRepository.existsByName("Non-existent Model");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Given saved threat model, when checking exists with different case, should be case sensitive")
        void givenSavedThreatModel_whenCheckingExistsWithDifferentCase_shouldBeCaseSensitive() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            threatModelRepository.save(threatModel);

            boolean exists = threatModelRepository.existsByName("payment system threat model");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Given saved threat model, when deleting, should remove from database")
        void givenSavedThreatModel_whenDeleting_shouldRemoveFromDatabase() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            ThreatModel saved = threatModelRepository.save(threatModel);

            threatModelRepository.deleteById(saved.getId());

            Optional<ThreatModel> found = threatModelRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Given multiple threat models, when deleting one, should only remove that one")
        void givenMultipleThreatModels_whenDeletingOne_shouldOnlyRemoveThatOne() {
            ThreatModel model1 = threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            ThreatModel model2 = threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());

            threatModelRepository.deleteById(model1.getId());

            List<ThreatModel> remaining = threatModelRepository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getId()).isEqualTo(model2.getId());
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Given empty database, when counting, should return zero")
        void givenEmptyDatabase_whenCounting_shouldReturnZero() {
            long count = threatModelRepository.count();

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Given multiple threat models, when counting, should return correct count")
        void givenMultipleThreatModels_whenCounting_shouldReturnCorrectCount() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.anApiThreatModel().build());

            long count = threatModelRepository.count();

            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Given saved threat model, when updating name, should persist changes")
        void givenSavedThreatModel_whenUpdatingName_shouldPersistChanges() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            ThreatModel saved = threatModelRepository.save(threatModel);

            saved.setName("Updated Payment System");
            threatModelRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            Optional<ThreatModel> updated = threatModelRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("Updated Payment System");
        }

        @Test
        @DisplayName("Given saved threat model, when updating description, should persist changes")
        void givenSavedThreatModel_whenUpdatingDescription_shouldPersistChanges() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            ThreatModel saved = threatModelRepository.save(threatModel);

            saved.setDescription("Updated description");
            threatModelRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            Optional<ThreatModel> updated = threatModelRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getDescription()).isEqualTo("Updated description");
        }
    }

    @Nested
    @DisplayName("Unique Name Constraint Tests")
    class UniqueNameConstraintTests {

        @Test
        @DisplayName("Given saved threat model, when saving another with same name, should handle duplicate")
        void givenSavedThreatModel_whenSavingAnotherWithSameName_shouldHandleDuplicate() {
            ThreatModel threatModel1 = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            threatModelRepository.save(threatModel1);
            entityManager.flush();

            ThreatModel threatModel2 = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();

            assertThat(threatModelRepository.existsByName(threatModel2.getName())).isTrue();
        }
    }

    @Nested
    @DisplayName("Null Description Tests")
    class NullDescriptionTests {

        @Test
        @DisplayName("Given threat model with null description, when saving, should persist successfully")
        void givenThreatModelWithNullDescription_whenSaving_shouldPersistSuccessfully() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aThreatModel()
                    .withName("Model Without Description")
                    .withNullDescription()
                    .build();

            ThreatModel saved = threatModelRepository.save(threatModel);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Search Threat Models Tests")
    class SearchThreatModelsTests {

        @Test
        @DisplayName("Given null search term, when searching, should return all threat models")
        void givenNullSearchTerm_whenSearching_shouldReturnAllThreatModels() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.anApiThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels(null);

            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("Given empty search term, when searching, should return all threat models")
        void givenEmptySearchTerm_whenSearching_shouldReturnAllThreatModels() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels(null);

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Given search term matching name, when searching, should return matching models")
        void givenSearchTermMatchingName_whenSearching_shouldReturnMatchingModels() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.anApiThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels("%payment%");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).contains("Payment");
        }

        @Test
        @DisplayName("Given search term matching description, when searching, should return matching models")
        void givenSearchTermMatchingDescription_whenSearching_shouldReturnMatchingModels() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels("%payment processing%");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).containsIgnoringCase("payment processing");
        }

        @Test
        @DisplayName("Given search term with different case, when searching, should be case insensitive")
        void givenSearchTermWithDifferentCase_whenSearching_shouldBeCaseInsensitive() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels("%payment%");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).containsIgnoringCase("payment");
        }

        @Test
        @DisplayName("Given search term with partial match, when searching, should return matching models")
        void givenSearchTermWithPartialMatch_whenSearching_shouldReturnMatchingModels() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.anApiThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels("%system%");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).contains("System");
        }

        @Test
        @DisplayName("Given search term with no matches, when searching, should return empty list")
        void givenSearchTermWithNoMatches_whenSearching_shouldReturnEmptyList() {
            threatModelRepository.save(ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build());
            threatModelRepository.save(ThreatModelTestDataBuilder.aWebApplicationThreatModel().build());

            List<ThreatModel> results = threatModelRepository.searchThreatModels("NonExistent");

            assertThat(results).isEmpty();
        }
    }
}
