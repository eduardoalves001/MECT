package com.ua.rtmp.service;

import com.ua.rtmp.exception.DuplicateResourceException;
import com.ua.rtmp.exception.ResourceNotFoundException;
import com.ua.rtmp.model.Component;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.repository.ComponentRepository;
import com.ua.rtmp.repository.ThreatModelRepository;
import com.ua.rtmp.util.ComponentTestDataBuilder;
import com.ua.rtmp.util.ThreatModelTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Component Service Tests")
class ComponentServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private ThreatModelRepository threatModelRepository;

    @InjectMocks
    private ComponentService componentService;

    @Nested
    @DisplayName("Service Configuration Tests")
    class ServiceConfigurationTests {

        @Test
        @DisplayName("Given service dependencies, when service is created, should inject all repositories")
        void givenServiceDependencies_whenServiceIsCreated_shouldInjectAllRepositories() {
            assertThat(componentService).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get All Components By Threat Model Id Tests")
    class GetAllComponentsByThreatModelIdTests {

        @Test
        @DisplayName("Given components exist for threat model, when getting all by threat model id, should return all components")
        void givenComponentsExistForThreatModel_whenGettingAllByThreatModelId_shouldReturnAllComponents() {
            UUID threatModelId = UUID.randomUUID();
            List<Component> expectedComponents = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build(),
                    ComponentTestDataBuilder.aDatabaseComponent().build()
            );
            when(componentRepository.findByThreatModelId(threatModelId)).thenReturn(expectedComponents);

            List<Component> result = componentService.getAllComponentsByThreatModelId(threatModelId);

            assertThat(result).hasSize(2);
            verify(componentRepository).findByThreatModelId(threatModelId);
        }

        @Test
        @DisplayName("Given no components exist for threat model, when getting all by threat model id, should return empty list")
        void givenNoComponentsExistForThreatModel_whenGettingAllByThreatModelId_shouldReturnEmptyList() {
            UUID threatModelId = UUID.randomUUID();
            when(componentRepository.findByThreatModelId(threatModelId)).thenReturn(List.of());

            List<Component> result = componentService.getAllComponentsByThreatModelId(threatModelId);

            assertThat(result).isEmpty();
            verify(componentRepository).findByThreatModelId(threatModelId);
        }
    }

    @Nested
    @DisplayName("Search Components Tests")
    class SearchComponentsTests {

        @Test
        @DisplayName("Given search term, when searching components, should use search query")
        void givenSearchTerm_whenSearchingComponents_shouldUseSearchQuery() {
            UUID threatModelId = UUID.randomUUID();
            String searchTerm = "Server";
            List<Component> expectedComponents = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build()
            );
            when(componentRepository.searchComponentsByThreatModelId(threatModelId, searchTerm))
                    .thenReturn(expectedComponents);

            List<Component> result = componentService.searchComponents(threatModelId, searchTerm);

            assertThat(result).hasSize(1);
            verify(componentRepository).searchComponentsByThreatModelId(threatModelId, searchTerm);
            verify(componentRepository, never()).findByThreatModelId(any());
        }

        @Test
        @DisplayName("Given null search term, when searching components, should return all components")
        void givenNullSearchTerm_whenSearchingComponents_shouldReturnAllComponents() {
            UUID threatModelId = UUID.randomUUID();
            List<Component> expectedComponents = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build(),
                    ComponentTestDataBuilder.aDatabaseComponent().build()
            );
            when(componentRepository.findByThreatModelId(threatModelId)).thenReturn(expectedComponents);

            List<Component> result = componentService.searchComponents(threatModelId, null);

            assertThat(result).hasSize(2);
            verify(componentRepository).findByThreatModelId(threatModelId);
            verify(componentRepository, never()).searchComponentsByThreatModelId(any(), any());
        }

        @Test
        @DisplayName("Given empty search term, when searching components, should return all components")
        void givenEmptySearchTerm_whenSearchingComponents_shouldReturnAllComponents() {
            UUID threatModelId = UUID.randomUUID();
            List<Component> expectedComponents = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build(),
                    ComponentTestDataBuilder.aDatabaseComponent().build()
            );
            when(componentRepository.findByThreatModelId(threatModelId)).thenReturn(expectedComponents);

            List<Component> result = componentService.searchComponents(threatModelId, "");

            assertThat(result).hasSize(2);
            verify(componentRepository).findByThreatModelId(threatModelId);
        }

        @Test
        @DisplayName("Given whitespace search term, when searching components, should return all components")
        void givenWhitespaceSearchTerm_whenSearchingComponents_shouldReturnAllComponents() {
            UUID threatModelId = UUID.randomUUID();
            List<Component> expectedComponents = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build()
            );
            when(componentRepository.findByThreatModelId(threatModelId)).thenReturn(expectedComponents);

            List<Component> result = componentService.searchComponents(threatModelId, "   ");

            assertThat(result).hasSize(1);
            verify(componentRepository).findByThreatModelId(threatModelId);
        }

        @Test
        @DisplayName("Given search term with leading/trailing spaces, when searching components, should trim and search")
        void givenSearchTermWithLeadingTrailingSpaces_whenSearchingComponents_shouldTrimAndSearch() {
            UUID threatModelId = UUID.randomUUID();
            String searchTerm = "  Server  ";
            List<Component> expectedComponents = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build()
            );
            when(componentRepository.searchComponentsByThreatModelId(threatModelId, "Server"))
                    .thenReturn(expectedComponents);

            List<Component> result = componentService.searchComponents(threatModelId, searchTerm);

            assertThat(result).hasSize(1);
            verify(componentRepository).searchComponentsByThreatModelId(threatModelId, "Server");
        }
    }

    @Nested
    @DisplayName("Get Component By Id Tests")
    class GetComponentByIdTests {

        @Test
        @DisplayName("Given component exists, when getting by id, should return component")
        void givenComponentExists_whenGettingById_shouldReturnComponent() {
            UUID componentId = UUID.randomUUID();
            Component expectedComponent = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .build();
            when(componentRepository.findById(componentId)).thenReturn(Optional.of(expectedComponent));

            Component result = componentService.getComponentById(componentId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(componentId);
            verify(componentRepository).findById(componentId);
        }

        @Test
        @DisplayName("Given component does not exist, when getting by id, should throw exception")
        void givenComponentDoesNotExist_whenGettingById_shouldThrowException() {
            UUID componentId = UUID.randomUUID();
            when(componentRepository.findById(componentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> componentService.getComponentById(componentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Component not found with id: " + componentId);

            verify(componentRepository).findById(componentId);
        }
    }

    @Nested
    @DisplayName("Create Component Tests")
    class CreateComponentTests {

        @Test
        @DisplayName("Given valid component data with unique name, when creating, should save and return component")
        void givenValidComponentDataWithUniqueName_whenCreating_shouldSaveAndReturnComponent() {
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            String name = "New Component";
            String description = "Component description";

            when(threatModelRepository.findById(threatModelId)).thenReturn(Optional.of(threatModel));
            when(componentRepository.existsByNameAndThreatModelId(name, threatModelId)).thenReturn(false);
            when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> {
                Component saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            Component result = componentService.createComponent(threatModelId, name, description);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getThreatModel()).isEqualTo(threatModel);
            verify(threatModelRepository).findById(threatModelId);
            verify(componentRepository).existsByNameAndThreatModelId(name, threatModelId);
            verify(componentRepository).save(any(Component.class));
        }

        @Test
        @DisplayName("Given threat model does not exist, when creating component, should throw exception")
        void givenThreatModelDoesNotExist_whenCreatingComponent_shouldThrowException() {
            UUID threatModelId = UUID.randomUUID();
            String name = "New Component";
            String description = "Component description";

            when(threatModelRepository.findById(threatModelId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> componentService.createComponent(threatModelId, name, description))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("ThreatModel not found with id: " + threatModelId);

            verify(threatModelRepository).findById(threatModelId);
            verify(componentRepository, never()).existsByNameAndThreatModelId(any(), any());
            verify(componentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given duplicate component name in same threat model, when creating, should throw exception")
        void givenDuplicateComponentNameInSameThreatModel_whenCreating_shouldThrowException() {
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            String name = "Existing Component";
            String description = "Component description";

            when(threatModelRepository.findById(threatModelId)).thenReturn(Optional.of(threatModel));
            when(componentRepository.existsByNameAndThreatModelId(name, threatModelId)).thenReturn(true);

            assertThatThrownBy(() -> componentService.createComponent(threatModelId, name, description))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Component with name '" + name + "' already exists in this threat model");

            verify(threatModelRepository).findById(threatModelId);
            verify(componentRepository).existsByNameAndThreatModelId(name, threatModelId);
            verify(componentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given component with null description, when creating, should save successfully")
        void givenComponentWithNullDescription_whenCreating_shouldSaveSuccessfully() {
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            String name = "Component Without Description";

            when(threatModelRepository.findById(threatModelId)).thenReturn(Optional.of(threatModel));
            when(componentRepository.existsByNameAndThreatModelId(name, threatModelId)).thenReturn(false);
            when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> {
                Component saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            Component result = componentService.createComponent(threatModelId, name, null);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getDescription()).isNull();
            verify(componentRepository).save(any(Component.class));
        }

        @Test
        @DisplayName("Given valid component, when creating, should establish bidirectional relationship with threat model")
        void givenValidComponent_whenCreating_shouldEstablishBidirectionalRelationshipWithThreatModel() {
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            String name = "New Component";
            String description = "Component description";

            when(threatModelRepository.findById(threatModelId)).thenReturn(Optional.of(threatModel));
            when(componentRepository.existsByNameAndThreatModelId(name, threatModelId)).thenReturn(false);
            when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> invocation.getArgument(0));

            componentService.createComponent(threatModelId, name, description);

            ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);
            verify(componentRepository).save(componentCaptor.capture());
            Component savedComponent = componentCaptor.getValue();
            assertThat(savedComponent.getThreatModel()).isEqualTo(threatModel);
            assertThat(threatModel.getComponents()).contains(savedComponent);
        }
    }

    @Nested
    @DisplayName("Update Component Tests")
    class UpdateComponentTests {

        @Test
        @DisplayName("Given component exists with unique new name, when updating, should save and return updated component")
        void givenComponentExistsWithUniqueNewName_whenUpdating_shouldSaveAndReturnUpdatedComponent() {
            UUID componentId = UUID.randomUUID();
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            Component existingComponent = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .withThreatModel(threatModel)
                    .build();
            String newName = "Updated Component";
            String newDescription = "Updated description";

            when(componentRepository.findById(componentId)).thenReturn(Optional.of(existingComponent));
            when(componentRepository.existsByNameAndThreatModelId(newName, threatModelId)).thenReturn(false);
            when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Component result = componentService.updateComponent(componentId, newName, newDescription);

            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getDescription()).isEqualTo(newDescription);
            verify(componentRepository).findById(componentId);
            verify(componentRepository).existsByNameAndThreatModelId(newName, threatModelId);
            verify(componentRepository).save(existingComponent);
        }

        @Test
        @DisplayName("Given component exists with same name, when updating, should not check for duplicates and save")
        void givenComponentExistsWithSameName_whenUpdating_shouldNotCheckForDuplicatesAndSave() {
            UUID componentId = UUID.randomUUID();
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            String sameName = "Web Server";
            Component existingComponent = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .withThreatModel(threatModel)
                    .build();
            String newDescription = "Updated description";

            when(componentRepository.findById(componentId)).thenReturn(Optional.of(existingComponent));
            when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Component result = componentService.updateComponent(componentId, sameName, newDescription);

            assertThat(result.getName()).isEqualTo(sameName);
            assertThat(result.getDescription()).isEqualTo(newDescription);
            verify(componentRepository).findById(componentId);
            verify(componentRepository, never()).existsByNameAndThreatModelId(any(), any());
            verify(componentRepository).save(existingComponent);
        }

        @Test
        @DisplayName("Given component does not exist, when updating, should throw exception")
        void givenComponentDoesNotExist_whenUpdating_shouldThrowException() {
            UUID componentId = UUID.randomUUID();
            when(componentRepository.findById(componentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> componentService.updateComponent(componentId, "New Name", "New Description"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Component not found with id: " + componentId);

            verify(componentRepository).findById(componentId);
            verify(componentRepository, never()).existsByNameAndThreatModelId(any(), any());
            verify(componentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given duplicate component name in same threat model, when updating, should throw exception")
        void givenDuplicateComponentNameInSameThreatModel_whenUpdating_shouldThrowException() {
            UUID componentId = UUID.randomUUID();
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            Component existingComponent = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .withThreatModel(threatModel)
                    .build();
            String duplicateName = "Existing Component";

            when(componentRepository.findById(componentId)).thenReturn(Optional.of(existingComponent));
            when(componentRepository.existsByNameAndThreatModelId(duplicateName, threatModelId)).thenReturn(true);

            assertThatThrownBy(() -> componentService.updateComponent(componentId, duplicateName, "Description"))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Component with name '" + duplicateName + "' already exists in this threat model");

            verify(componentRepository).findById(componentId);
            verify(componentRepository).existsByNameAndThreatModelId(duplicateName, threatModelId);
            verify(componentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given component with null description, when updating, should save successfully")
        void givenComponentWithNullDescription_whenUpdating_shouldSaveSuccessfully() {
            UUID componentId = UUID.randomUUID();
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            Component existingComponent = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .withThreatModel(threatModel)
                    .build();
            String newName = "Updated Component";

            when(componentRepository.findById(componentId)).thenReturn(Optional.of(existingComponent));
            when(componentRepository.existsByNameAndThreatModelId(newName, threatModelId)).thenReturn(false);
            when(componentRepository.save(any(Component.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Component result = componentService.updateComponent(componentId, newName, null);

            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getDescription()).isNull();
            verify(componentRepository).save(existingComponent);
        }
    }

    @Nested
    @DisplayName("Delete Component Tests")
    class DeleteComponentTests {

        @Test
        @DisplayName("Given component exists, when deleting, should remove component")
        void givenComponentExists_whenDeleting_shouldRemoveComponent() {
            UUID componentId = UUID.randomUUID();
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .withThreatModel(threatModel)
                    .build();
            threatModel.addComponent(component);

            when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));

            componentService.deleteComponent(componentId);

            verify(componentRepository).findById(componentId);
            verify(componentRepository).delete(component);
            assertThat(threatModel.getComponents()).doesNotContain(component);
        }

        @Test
        @DisplayName("Given component does not exist, when deleting, should throw exception")
        void givenComponentDoesNotExist_whenDeleting_shouldThrowException() {
            UUID componentId = UUID.randomUUID();
            when(componentRepository.findById(componentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> componentService.deleteComponent(componentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Component not found with id: " + componentId);

            verify(componentRepository).findById(componentId);
            verify(componentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Given component exists, when deleting, should break bidirectional relationship with threat model")
        void givenComponentExists_whenDeleting_shouldBreakBidirectionalRelationshipWithThreatModel() {
            UUID componentId = UUID.randomUUID();
            UUID threatModelId = UUID.randomUUID();
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel()
                    .withId(threatModelId)
                    .build();
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .withThreatModel(threatModel)
                    .build();
            threatModel.addComponent(component);

            when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));

            componentService.deleteComponent(componentId);

            assertThat(threatModel.getComponents()).doesNotContain(component);
            assertThat(component.getThreatModel()).isNull();
            verify(componentRepository).delete(component);
        }
    }
}
