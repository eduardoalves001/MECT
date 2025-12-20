package com.ua.rtmp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.rtmp.config.TestFlagsmithConfig;
import com.ua.rtmp.dto.response.RiskDistributionDTO;
import com.ua.rtmp.dto.response.ThreatModelStatsDTO;
import com.ua.rtmp.dto.response.ThreatsByCategoryDTO;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.enums.StrideCategory;
import com.ua.rtmp.model.enums.ThreatModelFilter;
import com.ua.rtmp.service.ThreatModelService;
import com.ua.rtmp.util.ThreatModelTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
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

@WebMvcTest(value = ThreatModelResource.class)
@Import({TestFlagsmithConfig.class, com.ua.rtmp.config.TestSecurityConfig.class, com.ua.rtmp.service.FeatureFlagService.class, com.ua.rtmp.config.MaintenanceModeInterceptor.class, com.ua.rtmp.config.WebConfig.class})
@DisplayName("ThreatModel Resource Tests")
class ThreatModelResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ThreatModelService threatModelService;

    @MockBean
    private com.ua.rtmp.service.ExportService exportService;

    private static final String BASE_URL = "/api/v1/threat-models";

    @Nested
    @DisplayName("GET /api/v1/threat-models - Get All Threat Models")
    class GetAllThreatModelsTests {

        @Test
        @DisplayName("Given threat models exist, when getting all, should return 200 with threat models")
        void givenThreatModelsExist_whenGettingAll_shouldReturn200WithThreatModels() throws Exception {
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build(),
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );
            when(threatModelService.searchThreatModels(null, ThreatModelFilter.ALL)).thenReturn(models);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threat models retrieved successfully")))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].name", is("Payment System Threat Model")))
                    .andExpect(jsonPath("$.data[1].name", is("Web Application Threat Model")));

            verify(threatModelService).searchThreatModels(null, ThreatModelFilter.ALL);
        }

        @Test
        @DisplayName("Given no threat models exist, when getting all, should return 200 with empty list")
        void givenNoThreatModelsExist_whenGettingAll_shouldReturn200WithEmptyList() throws Exception {
            when(threatModelService.searchThreatModels(null, ThreatModelFilter.ALL)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(threatModelService).searchThreatModels(null, ThreatModelFilter.ALL);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/threat-models/{id} - Get Threat Model By Id")
    class GetThreatModelByIdTests {

        @Test
        @DisplayName("Given threat model exists, when getting by id, should return 200 with threat model")
        void givenThreatModelExists_whenGettingById_shouldReturn200WithThreatModel() throws Exception {
            UUID modelId = UUID.randomUUID();
            ThreatModel model = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withId(modelId).build();
            when(threatModelService.getThreatModelById(modelId)).thenReturn(Optional.of(model));

            mockMvc.perform(get(BASE_URL + "/{id}", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threat model retrieved successfully")))
                    .andExpect(jsonPath("$.data.name", is("Payment System Threat Model")));

            verify(threatModelService).getThreatModelById(modelId);
        }

        @Test
        @DisplayName("Given threat model does not exist, when getting by id, should return 404")
        void givenThreatModelDoesNotExist_whenGettingById_shouldReturn404() throws Exception {
            UUID modelId = UUID.randomUUID();
            when(threatModelService.getThreatModelById(modelId)).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/{id}", modelId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("Threat model not found")));

            verify(threatModelService).getThreatModelById(modelId);
        }

        @Test
        @DisplayName("Given threat model with null description, when getting by id, should return 200")
        void givenThreatModelWithNullDescription_whenGettingById_shouldReturn200() throws Exception {
            UUID modelId = UUID.randomUUID();
            ThreatModel model = ThreatModelTestDataBuilder.aThreatModel()
                    .withId(modelId)
                    .withName("Model Without Description")
                    .withNullDescription()
                    .build();
            when(threatModelService.getThreatModelById(modelId)).thenReturn(Optional.of(model));

            mockMvc.perform(get(BASE_URL + "/{id}", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.description").doesNotExist());

            verify(threatModelService).getThreatModelById(modelId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/threat-models - Create Threat Model")
    class CreateThreatModelTests {

        @Test
        @DisplayName("Given valid threat model, when creating, should return 201")
        void givenValidThreatModel_whenCreating_shouldReturn201() throws Exception {
            ThreatModel inputModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            ThreatModel createdModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withRandomId().build();
            when(threatModelService.createThreatModel(any(ThreatModel.class))).thenReturn(createdModel);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inputModel)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threat model created successfully")))
                    .andExpect(jsonPath("$.data.name", is("Payment System Threat Model")));

            verify(threatModelService).createThreatModel(any(ThreatModel.class));
        }

        @Test
        @DisplayName("Given threat model with blank name, when creating, should return 400")
        void givenThreatModelWithBlankName_whenCreating_shouldReturn400() throws Exception {
            ThreatModel invalidModel = ThreatModelTestDataBuilder.aThreatModel().withBlankName().build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidModel)))
                    .andExpect(status().isBadRequest());

            verify(threatModelService, never()).createThreatModel(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/threat-models/{id} - Update Threat Model")
    class UpdateThreatModelTests {

        @Test
        @DisplayName("Given valid update, when updating, should return 200")
        void givenValidUpdate_whenUpdating_shouldReturn200() throws Exception {
            UUID modelId = UUID.randomUUID();
            ThreatModel updateData = ThreatModelTestDataBuilder.aWebApplicationThreatModel().build();
            ThreatModel updatedModel = ThreatModelTestDataBuilder.aWebApplicationThreatModel().withId(modelId).build();
            when(threatModelService.updateThreatModel(eq(modelId), any(ThreatModel.class)))
                    .thenReturn(Optional.of(updatedModel));

            mockMvc.perform(put(BASE_URL + "/{id}", modelId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threat model updated successfully")))
                    .andExpect(jsonPath("$.data.name", is("Web Application Threat Model")));

            verify(threatModelService).updateThreatModel(eq(modelId), any(ThreatModel.class));
        }

        @Test
        @DisplayName("Given non-existent id, when updating, should return 404")
        void givenNonExistentId_whenUpdating_shouldReturn404() throws Exception {
            UUID modelId = UUID.randomUUID();
            ThreatModel updateData = ThreatModelTestDataBuilder.aWebApplicationThreatModel().build();
            when(threatModelService.updateThreatModel(eq(modelId), any(ThreatModel.class)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(put(BASE_URL + "/{id}", modelId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateData)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("Threat model not found")));

            verify(threatModelService).updateThreatModel(eq(modelId), any(ThreatModel.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/threat-models/{id} - Delete Threat Model")
    class DeleteThreatModelTests {

        @Test
        @DisplayName("Given existing threat model, when deleting, should return 200")
        void givenExistingThreatModel_whenDeleting_shouldReturn200() throws Exception {
            UUID modelId = UUID.randomUUID();
            when(threatModelService.deleteThreatModel(modelId)).thenReturn(true);

            mockMvc.perform(delete(BASE_URL + "/{id}", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threat model deleted successfully")));

            verify(threatModelService).deleteThreatModel(modelId);
        }

        @Test
        @DisplayName("Given non-existent threat model, when deleting, should return 404")
        void givenNonExistentThreatModel_whenDeleting_shouldReturn404() throws Exception {
            UUID modelId = UUID.randomUUID();
            when(threatModelService.deleteThreatModel(modelId)).thenReturn(false);

            mockMvc.perform(delete(BASE_URL + "/{id}", modelId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", is("Threat model not found")));

            verify(threatModelService).deleteThreatModel(modelId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/threat-models/{id}/stats - Get Threat Model Stats")
    class GetThreatModelStatsTests {

        @Test
        @DisplayName("Given threat model exists, when getting stats, should return 200 with statistics")
        void givenThreatModelExists_whenGettingStats_shouldReturn200WithStatistics() throws Exception {
            UUID modelId = UUID.randomUUID();
            ThreatModelStatsDTO stats = new ThreatModelStatsDTO(5, 8, 2, 3);
            when(threatModelService.getThreatModelStats(modelId)).thenReturn(stats);

            mockMvc.perform(get(BASE_URL + "/{id}/stats", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threat model statistics retrieved successfully")))
                    .andExpect(jsonPath("$.data.totalComponents", is(5)))
                    .andExpect(jsonPath("$.data.activeThreats", is(8)))
                    .andExpect(jsonPath("$.data.highRiskThreats", is(2)))
                    .andExpect(jsonPath("$.data.mitigatedThreats", is(3)));

            verify(threatModelService).getThreatModelStats(modelId);
        }

        @Test
        @DisplayName("Given threat model with no data, when getting stats, should return 200 with zeros")
        void givenThreatModelWithNoData_whenGettingStats_shouldReturn200WithZeros() throws Exception {
            UUID modelId = UUID.randomUUID();
            ThreatModelStatsDTO stats = new ThreatModelStatsDTO(0, 0, 0, 0);
            when(threatModelService.getThreatModelStats(modelId)).thenReturn(stats);

            mockMvc.perform(get(BASE_URL + "/{id}/stats", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalComponents", is(0)))
                    .andExpect(jsonPath("$.data.activeThreats", is(0)))
                    .andExpect(jsonPath("$.data.highRiskThreats", is(0)))
                    .andExpect(jsonPath("$.data.mitigatedThreats", is(0)));

            verify(threatModelService).getThreatModelStats(modelId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/threat-models/{id}/threats-by-category - Get Threats By Category")
    class GetThreatsByCategoryTests {

        @Test
        @DisplayName("Given threat model with threats, when getting by category, should return 200 with distribution")
        void givenThreatModelWithThreats_whenGettingByCategory_shouldReturn200WithDistribution() throws Exception {
            UUID modelId = UUID.randomUUID();
            List<ThreatsByCategoryDTO> data = List.of(
                    new ThreatsByCategoryDTO(StrideCategory.SPOOFING, 3L),
                    new ThreatsByCategoryDTO(StrideCategory.TAMPERING, 2L)
            );
            when(threatModelService.getThreatsByCategory(modelId)).thenReturn(data);

            mockMvc.perform(get(BASE_URL + "/{id}/threats-by-category", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Threats by category retrieved successfully")))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].category", is("SPOOFING")))
                    .andExpect(jsonPath("$.data[0].count", is(3)))
                    .andExpect(jsonPath("$.data[1].category", is("TAMPERING")))
                    .andExpect(jsonPath("$.data[1].count", is(2)));

            verify(threatModelService).getThreatsByCategory(modelId);
        }

        @Test
        @DisplayName("Given threat model with no threats, when getting by category, should return 200 with empty list")
        void givenThreatModelWithNoThreats_whenGettingByCategory_shouldReturn200WithEmptyList() throws Exception {
            UUID modelId = UUID.randomUUID();
            when(threatModelService.getThreatsByCategory(modelId)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{id}/threats-by-category", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(threatModelService).getThreatsByCategory(modelId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/threat-models/{id}/risk-distribution - Get Risk Distribution")
    class GetRiskDistributionTests {

        @Test
        @DisplayName("Given threat model with risks, when getting distribution, should return 200 with risk levels")
        void givenThreatModelWithRisks_whenGettingDistribution_shouldReturn200WithRiskLevels() throws Exception {
            UUID modelId = UUID.randomUUID();
            List<RiskDistributionDTO> data = List.of(
                    new RiskDistributionDTO("Low", 5L),
                    new RiskDistributionDTO("Medium", 3L),
                    new RiskDistributionDTO("High", 2L)
            );
            when(threatModelService.getRiskDistribution(modelId)).thenReturn(data);

            mockMvc.perform(get(BASE_URL + "/{id}/risk-distribution", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.message", is("Risk distribution retrieved successfully")))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].riskLevel", is("Low")))
                    .andExpect(jsonPath("$.data[0].count", is(5)))
                    .andExpect(jsonPath("$.data[1].riskLevel", is("Medium")))
                    .andExpect(jsonPath("$.data[1].count", is(3)))
                    .andExpect(jsonPath("$.data[2].riskLevel", is("High")))
                    .andExpect(jsonPath("$.data[2].count", is(2)));

            verify(threatModelService).getRiskDistribution(modelId);
        }

        @Test
        @DisplayName("Given threat model with no risks, when getting distribution, should return 200 with empty list")
        void givenThreatModelWithNoRisks_whenGettingDistribution_shouldReturn200WithEmptyList() throws Exception {
            UUID modelId = UUID.randomUUID();
            when(threatModelService.getRiskDistribution(modelId)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{id}/risk-distribution", modelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(threatModelService).getRiskDistribution(modelId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/threat-models with Search and Filter - Search Threat Models")
    class SearchThreatModelsTests {

        @Test
        @DisplayName("Given search parameter, when getting threat models, should return filtered results")
        void givenSearchParameter_whenGettingThreatModels_shouldReturnFilteredResults() throws Exception {
            String searchTerm = "Payment";
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("search", searchTerm))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", is("Payment System Threat Model")));

            verify(threatModelService).searchThreatModels(searchTerm, ThreatModelFilter.ALL);
        }

        @Test
        @DisplayName("Given search with no matches, when getting threat models, should return empty list")
        void givenSearchWithNoMatches_whenGettingThreatModels_shouldReturnEmptyList() throws Exception {
            String searchTerm = "NonExistent";
            when(threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL)
                            .param("search", searchTerm))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(threatModelService).searchThreatModels(searchTerm, ThreatModelFilter.ALL);
        }

        @Test
        @DisplayName("Given ACTIVE_THREATS filter, when getting threat models, should return models with active threats")
        void givenActiveThreatsFilter_whenGettingThreatModels_shouldReturnModelsWithActiveThreats() throws Exception {
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelService.searchThreatModels(null, ThreatModelFilter.ACTIVE_THREATS)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("filter", "ACTIVE_THREATS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(threatModelService).searchThreatModels(null, ThreatModelFilter.ACTIVE_THREATS);
        }

        @Test
        @DisplayName("Given HIGH_RISK_THREATS filter, when getting threat models, should return models with high risk threats")
        void givenHighRiskThreatsFilter_whenGettingThreatModels_shouldReturnModelsWithHighRiskThreats() throws Exception {
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelService.searchThreatModels(null, ThreatModelFilter.HIGH_RISK_THREATS)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("filter", "HIGH_RISK_THREATS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(threatModelService).searchThreatModels(null, ThreatModelFilter.HIGH_RISK_THREATS);
        }

        @Test
        @DisplayName("Given MITIGATED_THREATS filter, when getting threat models, should return models with mitigated threats")
        void givenMitigatedThreatsFilter_whenGettingThreatModels_shouldReturnModelsWithMitigatedThreats() throws Exception {
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );
            when(threatModelService.searchThreatModels(null, ThreatModelFilter.MITIGATED_THREATS)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("filter", "MITIGATED_THREATS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(threatModelService).searchThreatModels(null, ThreatModelFilter.MITIGATED_THREATS);
        }

        @Test
        @DisplayName("Given search and filter parameters, when getting threat models, should apply both")
        void givenSearchAndFilter_whenGettingThreatModels_shouldApplyBoth() throws Exception {
            String searchTerm = "Payment";
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ACTIVE_THREATS)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("search", searchTerm)
                            .param("filter", "ACTIVE_THREATS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", is("Payment System Threat Model")));

            verify(threatModelService).searchThreatModels(searchTerm, ThreatModelFilter.ACTIVE_THREATS);
        }

        @Test
        @DisplayName("Given no filter parameter, when getting threat models, should default to ALL filter")
        void givenNoFilterParameter_whenGettingThreatModels_shouldDefaultToAllFilter() throws Exception {
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build(),
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );
            when(threatModelService.searchThreatModels(null, ThreatModelFilter.ALL)).thenReturn(models);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            verify(threatModelService).searchThreatModels(null, ThreatModelFilter.ALL);
        }

        @Test
        @DisplayName("Given empty search parameter, when getting threat models, should treat as no search")
        void givenEmptySearchParameter_whenGettingThreatModels_shouldTreatAsNoSearch() throws Exception {
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelService.searchThreatModels("", ThreatModelFilter.ALL)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("search", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(threatModelService).searchThreatModels("", ThreatModelFilter.ALL);
        }

        @Test
        @DisplayName("Given search with special characters, when getting threat models, should handle properly")
        void givenSearchWithSpecialCharacters_whenGettingThreatModels_shouldHandleProperly() throws Exception {
            String searchTerm = "Pay@ment#System";
            List<ThreatModel> models = List.of();
            when(threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("search", searchTerm))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(threatModelService).searchThreatModels(searchTerm, ThreatModelFilter.ALL);
        }

        @Test
        @DisplayName("Given case-insensitive search, when getting threat models, should return results")
        void givenCaseInsensitiveSearch_whenGettingThreatModels_shouldReturnResults() throws Exception {
            String searchTerm = "payment";
            List<ThreatModel> models = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL)).thenReturn(models);

            mockMvc.perform(get(BASE_URL)
                            .param("search", searchTerm))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(threatModelService).searchThreatModels(searchTerm, ThreatModelFilter.ALL);
        }
    }
}
