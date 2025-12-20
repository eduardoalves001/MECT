package com.ua.rtmp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.rtmp.config.TestFlagsmithConfig;
import com.ua.rtmp.model.Component;
import com.ua.rtmp.service.ComponentService;
import com.ua.rtmp.service.FeatureFlagService;
import com.ua.rtmp.util.ComponentTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ComponentResource.class)
@Import({TestFlagsmithConfig.class, com.ua.rtmp.config.TestSecurityConfig.class, com.ua.rtmp.service.FeatureFlagService.class, com.ua.rtmp.config.MaintenanceModeInterceptor.class, com.ua.rtmp.config.WebConfig.class})
@DisplayName("Component Resource Tests")
class ComponentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComponentService componentService;

    private static final String BASE_URL = "/api/v1/threat-models/{threatModelId}/components";

    @Nested
    @DisplayName("GET /api/v1/threat-models/{threatModelId}/components - Get All Components")
    class GetAllComponentsTests {

        @Test
        @DisplayName("Given components exist for threat model, when getting all, should return 200 with components")
        void givenComponentsExistForThreatModel_whenGettingAll_shouldReturn200WithComponents() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            List<Component> components = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build(),
                    ComponentTestDataBuilder.aDatabaseComponent().build()
            );
            when(componentService.searchComponents(threatModelId, null)).thenReturn(components);

            mockMvc.perform(get(BASE_URL, threatModelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Components retrieved successfully")))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].name", is("Web Server")))
                    .andExpect(jsonPath("$.data[1].name", is("Database Server")));

            verify(componentService).searchComponents(threatModelId, null);
        }

        @Test
        @DisplayName("Given no components exist for threat model, when getting all, should return 200 with empty list")
        void givenNoComponentsExistForThreatModel_whenGettingAll_shouldReturn200WithEmptyList() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            when(componentService.searchComponents(threatModelId, null)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL, threatModelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(componentService).searchComponents(threatModelId, null);
        }

        @Test
        @DisplayName("Given search parameter, when getting all components, should return filtered results")
        void givenSearchParameter_whenGettingAllComponents_shouldReturnFilteredResults() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            String searchTerm = "Server";
            List<Component> components = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build()
            );
            when(componentService.searchComponents(threatModelId, searchTerm)).thenReturn(components);

            mockMvc.perform(get(BASE_URL, threatModelId)
                            .param("search", searchTerm))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", is("Web Server")));

            verify(componentService).searchComponents(threatModelId, searchTerm);
        }

        @Test
        @DisplayName("Given empty search parameter, when getting all components, should return all components")
        void givenEmptySearchParameter_whenGettingAllComponents_shouldReturnAllComponents() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            List<Component> components = List.of(
                    ComponentTestDataBuilder.aWebServerComponent().build(),
                    ComponentTestDataBuilder.aDatabaseComponent().build()
            );
            when(componentService.searchComponents(threatModelId, "")).thenReturn(components);

            mockMvc.perform(get(BASE_URL, threatModelId)
                            .param("search", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            verify(componentService).searchComponents(threatModelId, "");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/threat-models/{threatModelId}/components/{id} - Get Component By Id")
    class GetComponentByIdTests {

        @Test
        @DisplayName("Given component exists, when getting by id, should return 200 with component")
        void givenComponentExists_whenGettingById_shouldReturn200WithComponent() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();
            Component component = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(componentId)
                    .build();
            when(componentService.getComponentById(componentId)).thenReturn(component);

            mockMvc.perform(get(BASE_URL + "/{id}", threatModelId, componentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Component retrieved successfully")))
                    .andExpect(jsonPath("$.data.name", is("Web Server")))
                    .andExpect(jsonPath("$.data.description", is("Handles HTTP requests and serves web content")));

            verify(componentService).getComponentById(componentId);
        }

        @Test
        @DisplayName("Given component with null description, when getting by id, should return 200")
        void givenComponentWithNullDescription_whenGettingById_shouldReturn200() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();
            Component component = ComponentTestDataBuilder.aComponent()
                    .withId(componentId)
                    .withName("Component Without Description")
                    .withNullDescription()
                    .build();
            when(componentService.getComponentById(componentId)).thenReturn(component);

            mockMvc.perform(get(BASE_URL + "/{id}", threatModelId, componentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.name", is("Component Without Description")));

            verify(componentService).getComponentById(componentId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/threat-models/{threatModelId}/components - Create Component")
    class CreateComponentTests {

        @Test
        @DisplayName("Given valid component data, when creating, should return 201 with created component")
        void givenValidComponentData_whenCreating_shouldReturn201WithCreatedComponent() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            Component inputComponent = ComponentTestDataBuilder.aWebServerComponent().build();
            Component createdComponent = ComponentTestDataBuilder.aWebServerComponent()
                    .withId(UUID.randomUUID())
                    .build();

            when(componentService.createComponent(eq(threatModelId), eq(inputComponent.getName()), eq(inputComponent.getDescription())))
                    .thenReturn(createdComponent);

            mockMvc.perform(post(BASE_URL, threatModelId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inputComponent)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Component created successfully")))
                    .andExpect(jsonPath("$.data.name", is("Web Server")))
                    .andExpect(jsonPath("$.data.id").exists());

            verify(componentService).createComponent(threatModelId, inputComponent.getName(), inputComponent.getDescription());
        }

        @Test
        @DisplayName("Given invalid component data with blank name, when creating, should return 400")
        void givenInvalidComponentDataWithBlankName_whenCreating_shouldReturn400() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            Component invalidComponent = ComponentTestDataBuilder.aComponent()
                    .withBlankName()
                    .build();

            mockMvc.perform(post(BASE_URL, threatModelId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidComponent)))
                    .andExpect(status().isBadRequest());

            verify(componentService, never()).createComponent(any(), any(), any());
        }

        @Test
        @DisplayName("Given component with null description, when creating, should return 201")
        void givenComponentWithNullDescription_whenCreating_shouldReturn201() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            Component inputComponent = ComponentTestDataBuilder.aComponent()
                    .withName("Component Without Description")
                    .withNullDescription()
                    .build();
            Component createdComponent = ComponentTestDataBuilder.aComponent()
                    .withId(UUID.randomUUID())
                    .withName("Component Without Description")
                    .withNullDescription()
                    .build();

            when(componentService.createComponent(eq(threatModelId), eq(inputComponent.getName()), eq(inputComponent.getDescription())))
                    .thenReturn(createdComponent);

            mockMvc.perform(post(BASE_URL, threatModelId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inputComponent)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.name", is("Component Without Description")));

            verify(componentService).createComponent(threatModelId, inputComponent.getName(), null);
        }

        @Test
        @DisplayName("Given component with name exceeding max length, when creating, should return 400")
        void givenComponentWithNameExceedingMaxLength_whenCreating_shouldReturn400() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            Component invalidComponent = ComponentTestDataBuilder.aComponent()
                    .withTooLongName()
                    .build();

            mockMvc.perform(post(BASE_URL, threatModelId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidComponent)))
                    .andExpect(status().isBadRequest());

            verify(componentService, never()).createComponent(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/threat-models/{threatModelId}/components/{id} - Update Component")
    class UpdateComponentTests {

        @Test
        @DisplayName("Given valid update data, when updating, should return 200 with updated component")
        void givenValidUpdateData_whenUpdating_shouldReturn200WithUpdatedComponent() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();
            Component updateData = ComponentTestDataBuilder.aComponent()
                    .withName("Updated Component")
                    .withDescription("Updated description")
                    .build();
            Component updatedComponent = ComponentTestDataBuilder.aComponent()
                    .withId(componentId)
                    .withName("Updated Component")
                    .withDescription("Updated description")
                    .build();

            when(componentService.updateComponent(eq(componentId), eq(updateData.getName()), eq(updateData.getDescription())))
                    .thenReturn(updatedComponent);

            mockMvc.perform(put(BASE_URL + "/{id}", threatModelId, componentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Component updated successfully")))
                    .andExpect(jsonPath("$.data.name", is("Updated Component")))
                    .andExpect(jsonPath("$.data.description", is("Updated description")));

            verify(componentService).updateComponent(componentId, updateData.getName(), updateData.getDescription());
        }

        @Test
        @DisplayName("Given invalid update data with blank name, when updating, should return 400")
        void givenInvalidUpdateDataWithBlankName_whenUpdating_shouldReturn400() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();
            Component invalidUpdateData = ComponentTestDataBuilder.aComponent()
                    .withBlankName()
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}", threatModelId, componentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUpdateData)))
                    .andExpect(status().isBadRequest());

            verify(componentService, never()).updateComponent(any(), any(), any());
        }

        @Test
        @DisplayName("Given update data with null description, when updating, should return 200")
        void givenUpdateDataWithNullDescription_whenUpdating_shouldReturn200() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();
            Component updateData = ComponentTestDataBuilder.aComponent()
                    .withName("Updated Component")
                    .withNullDescription()
                    .build();
            Component updatedComponent = ComponentTestDataBuilder.aComponent()
                    .withId(componentId)
                    .withName("Updated Component")
                    .withNullDescription()
                    .build();

            when(componentService.updateComponent(eq(componentId), eq(updateData.getName()), eq(updateData.getDescription())))
                    .thenReturn(updatedComponent);

            mockMvc.perform(put(BASE_URL + "/{id}", threatModelId, componentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.name", is("Updated Component")));

            verify(componentService).updateComponent(componentId, updateData.getName(), null);
        }

        @Test
        @DisplayName("Given update data with name exceeding max length, when updating, should return 400")
        void givenUpdateDataWithNameExceedingMaxLength_whenUpdating_shouldReturn400() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();
            Component invalidUpdateData = ComponentTestDataBuilder.aComponent()
                    .withTooLongName()
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}", threatModelId, componentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUpdateData)))
                    .andExpect(status().isBadRequest());

            verify(componentService, never()).updateComponent(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/threat-models/{threatModelId}/components/{id} - Delete Component")
    class DeleteComponentTests {

        @Test
        @DisplayName("Given component exists, when deleting, should return 200")
        void givenComponentExists_whenDeleting_shouldReturn200() throws Exception {
            UUID threatModelId = UUID.randomUUID();
            UUID componentId = UUID.randomUUID();

            mockMvc.perform(delete(BASE_URL + "/{id}", threatModelId, componentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Component deleted successfully")))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(componentService).deleteComponent(componentId);
        }
    }
}
