package com.ua.rtmp.repository;

import com.ua.rtmp.model.Component;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.util.ComponentTestDataBuilder;
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
@DisplayName("Component Repository Tests")
class ComponentRepositoryTest {

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ThreatModelRepository threatModelRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ThreatModel savedThreatModel;

    @BeforeEach
    void setUp() {
        componentRepository.deleteAll();
        threatModelRepository.deleteAll();

        savedThreatModel = threatModelRepository.save(
                ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
        );
        entityManager.flush();
    }

    @Nested
    @DisplayName("Save and Retrieve Tests")
    class SaveAndRetrieveTests {

        @Test
        @DisplayName("Given valid component, when saving, should persist to database")
        void givenValidComponent_whenSaving_shouldPersistToDatabase() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();

            Component saved = componentRepository.save(component);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Web Server");
            assertThat(saved.getDescription()).isEqualTo("Handles HTTP requests and serves web content");
            assertThat(saved.getThreatModel()).isEqualTo(savedThreatModel);
        }

        @Test
        @DisplayName("Given saved component, when finding by id, should retrieve it")
        void givenSavedComponent_whenFindingById_shouldRetrieveIt() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component saved = componentRepository.save(component);
            entityManager.flush();
            entityManager.clear();

            Optional<Component> found = componentRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Web Server");
            assertThat(found.get().getThreatModel().getId()).isEqualTo(savedThreatModel.getId());
        }

        @Test
        @DisplayName("Given non-existent id, when finding by id, should return empty")
        void givenNonExistentId_whenFindingById_shouldReturnEmpty() {
            UUID nonExistentId = UUID.randomUUID();

            Optional<Component> found = componentRepository.findById(nonExistentId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Given multiple components, when finding all, should retrieve all")
        void givenMultipleComponents_whenFindingAll_shouldRetrieveAll() {
            Component comp1 = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component comp2 = ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component comp3 = ComponentTestDataBuilder.anApiGatewayComponent()
                    .withThreatModel(savedThreatModel)
                    .build();

            componentRepository.save(comp1);
            componentRepository.save(comp2);
            componentRepository.save(comp3);

            List<Component> allComponents = componentRepository.findAll();

            assertThat(allComponents).hasSize(3);
        }

        @Test
        @DisplayName("Given component with null description, when saving, should persist successfully")
        void givenComponentWithNullDescription_whenSaving_shouldPersistSuccessfully() {
            Component component = ComponentTestDataBuilder.aComponent()
                    .withName("Component Without Description")
                    .withNullDescription()
                    .withThreatModel(savedThreatModel)
                    .build();

            Component saved = componentRepository.save(component);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Find By Threat Model Id Tests")
    class FindByThreatModelIdTests {

        @Test
        @DisplayName("Given components for threat model, when finding by threat model id, should return all components")
        void givenComponentsForThreatModel_whenFindingByThreatModelId_shouldReturnAllComponents() {
            Component comp1 = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component comp2 = ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            componentRepository.save(comp1);
            componentRepository.save(comp2);

            List<Component> components = componentRepository.findByThreatModelId(savedThreatModel.getId());

            assertThat(components).hasSize(2);
            assertThat(components).extracting(Component::getName)
                    .containsExactlyInAnyOrder("Web Server", "Database Server");
        }

        @Test
        @DisplayName("Given multiple threat models with components, when finding by specific threat model id, should return only matching components")
        void givenMultipleThreatModelsWithComponents_whenFindingBySpecificThreatModelId_shouldReturnOnlyMatchingComponents() {
            ThreatModel anotherThreatModel = threatModelRepository.save(
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );

            Component comp1 = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component comp2 = ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(anotherThreatModel)
                    .build();
            componentRepository.save(comp1);
            componentRepository.save(comp2);

            List<Component> components = componentRepository.findByThreatModelId(savedThreatModel.getId());

            assertThat(components).hasSize(1);
            assertThat(components.get(0).getName()).isEqualTo("Web Server");
        }

        @Test
        @DisplayName("Given no components for threat model, when finding by threat model id, should return empty list")
        void givenNoComponentsForThreatModel_whenFindingByThreatModelId_shouldReturnEmptyList() {
            List<Component> components = componentRepository.findByThreatModelId(savedThreatModel.getId());

            assertThat(components).isEmpty();
        }

        @Test
        @DisplayName("Given non-existent threat model id, when finding by threat model id, should return empty list")
        void givenNonExistentThreatModelId_whenFindingByThreatModelId_shouldReturnEmptyList() {
            UUID nonExistentId = UUID.randomUUID();

            List<Component> components = componentRepository.findByThreatModelId(nonExistentId);

            assertThat(components).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists By Name And Threat Model Id Tests")
    class ExistsByNameAndThreatModelIdTests {

        @Test
        @DisplayName("Given saved component, when checking exists by name and threat model id, should return true")
        void givenSavedComponent_whenCheckingExistsByNameAndThreatModelId_shouldReturnTrue() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            componentRepository.save(component);

            boolean exists = componentRepository.existsByNameAndThreatModelId(
                    "Web Server",
                    savedThreatModel.getId()
            );

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Given non-existent name, when checking exists by name and threat model id, should return false")
        void givenNonExistentName_whenCheckingExistsByNameAndThreatModelId_shouldReturnFalse() {
            boolean exists = componentRepository.existsByNameAndThreatModelId(
                    "Non-existent Component",
                    savedThreatModel.getId()
            );

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Given component with same name in different threat model, when checking exists, should return false")
        void givenComponentWithSameNameInDifferentThreatModel_whenCheckingExists_shouldReturnFalse() {
            ThreatModel anotherThreatModel = threatModelRepository.save(
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(anotherThreatModel)
                    .build();
            componentRepository.save(component);

            boolean exists = componentRepository.existsByNameAndThreatModelId(
                    "Web Server",
                    savedThreatModel.getId()
            );

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Given saved component, when checking exists with different case, should be case sensitive")
        void givenSavedComponent_whenCheckingExistsWithDifferentCase_shouldBeCaseSensitive() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            componentRepository.save(component);

            boolean exists = componentRepository.existsByNameAndThreatModelId(
                    "web server",
                    savedThreatModel.getId()
            );

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Count By Threat Model Id Tests")
    class CountByThreatModelIdTests {

        @Test
        @DisplayName("Given components for threat model, when counting by threat model id, should return correct count")
        void givenComponentsForThreatModel_whenCountingByThreatModelId_shouldReturnCorrectCount() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.anApiGatewayComponent()
                    .withThreatModel(savedThreatModel).build());

            Integer count = componentRepository.countByThreatModelId(savedThreatModel.getId());

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Given no components for threat model, when counting by threat model id, should return zero")
        void givenNoComponentsForThreatModel_whenCountingByThreatModelId_shouldReturnZero() {
            Integer count = componentRepository.countByThreatModelId(savedThreatModel.getId());

            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("Given multiple threat models with components, when counting by specific threat model id, should return only matching count")
        void givenMultipleThreatModelsWithComponents_whenCountingBySpecificThreatModelId_shouldReturnOnlyMatchingCount() {
            ThreatModel anotherThreatModel = threatModelRepository.save(
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );

            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.anApiGatewayComponent()
                    .withThreatModel(anotherThreatModel).build());

            Integer count = componentRepository.countByThreatModelId(savedThreatModel.getId());

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Search Components Tests")
    class SearchComponentsTests {

        @Test
        @DisplayName("Given components, when searching by name keyword, should return matching components")
        void givenComponents_whenSearchingByNameKeyword_shouldReturnMatchingComponents() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.anApiGatewayComponent()
                    .withThreatModel(savedThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    "Server"
            );

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Component::getName)
                    .containsExactlyInAnyOrder("Web Server", "Database Server");
        }

        @Test
        @DisplayName("Given components, when searching by description keyword, should return matching components")
        void givenComponents_whenSearchingByDescriptionKeyword_shouldReturnMatchingComponents() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.anApiGatewayComponent()
                    .withThreatModel(savedThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    "data"
            );

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Database Server");
        }

        @Test
        @DisplayName("Given components, when searching with null keyword, should return all components")
        void givenComponents_whenSearchingWithNullKeyword_shouldReturnAllComponents() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    null
            );

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Given components, when searching with empty keyword, should return all components")
        void givenComponents_whenSearchingWithEmptyKeyword_shouldReturnAllComponents() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    ""
            );

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Given components, when searching with case-insensitive keyword, should return matching components")
        void givenComponents_whenSearchingWithCaseInsensitiveKeyword_shouldReturnMatchingComponents() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    "WEB"
            );

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Web Server");
        }

        @Test
        @DisplayName("Given components, when searching with no matches, should return empty list")
        void givenComponents_whenSearchingWithNoMatches_shouldReturnEmptyList() {
            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    "nonexistent"
            );

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Given multiple threat models with components, when searching, should return only components from specified threat model")
        void givenMultipleThreatModelsWithComponents_whenSearching_shouldReturnOnlyComponentsFromSpecifiedThreatModel() {
            ThreatModel anotherThreatModel = threatModelRepository.save(
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );

            componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            componentRepository.save(ComponentTestDataBuilder.aComponent()
                    .withName("Web Application")
                    .withThreatModel(anotherThreatModel).build());

            List<Component> results = componentRepository.searchComponentsByThreatModelId(
                    savedThreatModel.getId(),
                    "Web"
            );

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Web Server");
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Given saved component, when updating name, should persist changes")
        void givenSavedComponent_whenUpdatingName_shouldPersistChanges() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component saved = componentRepository.save(component);
            entityManager.flush();
            entityManager.clear();

            saved.setName("Updated Web Server");
            Component updated = componentRepository.save(saved);

            assertThat(updated.getName()).isEqualTo("Updated Web Server");
        }

        @Test
        @DisplayName("Given saved component, when updating description, should persist changes")
        void givenSavedComponent_whenUpdatingDescription_shouldPersistChanges() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component saved = componentRepository.save(component);
            entityManager.flush();
            entityManager.clear();

            saved.setDescription("Updated description");
            Component updated = componentRepository.save(saved);

            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Given saved component, when deleting, should remove from database")
        void givenSavedComponent_whenDeleting_shouldRemoveFromDatabase() {
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel)
                    .build();
            Component saved = componentRepository.save(component);

            componentRepository.deleteById(saved.getId());

            Optional<Component> found = componentRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Given multiple components, when deleting one, should only remove that one")
        void givenMultipleComponents_whenDeletingOne_shouldOnlyRemoveThatOne() {
            Component comp1 = componentRepository.save(ComponentTestDataBuilder.aWebServerComponent()
                    .withThreatModel(savedThreatModel).build());
            Component comp2 = componentRepository.save(ComponentTestDataBuilder.aDatabaseComponent()
                    .withThreatModel(savedThreatModel).build());

            componentRepository.deleteById(comp1.getId());

            List<Component> remaining = componentRepository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getId()).isEqualTo(comp2.getId());
        }

        @Test
        @DisplayName("Given non-existent component id, when deleting, should not throw exception")
        void givenNonExistentComponentId_whenDeleting_shouldNotThrowException() {
            UUID nonExistentId = UUID.randomUUID();

            componentRepository.deleteById(nonExistentId);

            assertThat(componentRepository.count()).isEqualTo(0);
        }
    }
}
