package com.ua.rtmp.service;

import com.ua.rtmp.dto.response.RiskDistributionDTO;
import com.ua.rtmp.dto.response.ThreatModelStatsDTO;
import com.ua.rtmp.dto.response.ThreatsByCategoryDTO;
import com.ua.rtmp.exception.DuplicateResourceException;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.enums.StrideCategory;
import com.ua.rtmp.model.enums.ThreatModelFilter;
import com.ua.rtmp.repository.ComponentRepository;
import com.ua.rtmp.repository.ThreatModelRepository;
import com.ua.rtmp.repository.VulnerabilityRepository;
import com.ua.rtmp.util.ThreatModelTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThreatModel Service Tests")
class ThreatModelServiceTest {

    @Mock
    private ThreatModelRepository threatModelRepository;

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private VulnerabilityRepository vulnerabilityRepository;

    @InjectMocks
    private ThreatModelService threatModelService;

    @Nested
    @DisplayName("Service Configuration Tests")
    class ServiceConfigurationTests {

        @Test
        @DisplayName("Given service dependencies, when service is created, should inject all repositories")
        void givenServiceDependencies_whenServiceIsCreated_shouldInjectAllRepositories() {
            assertThat(threatModelService).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get All Threat Models Tests")
    class GetAllThreatModelsTests {

        @Test
        @DisplayName("Given threat models exist, when getting all, should return all threat models")
        void givenThreatModelsExist_whenGettingAll_shouldReturnAllThreatModels() {
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build(),
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );
            when(threatModelRepository.findAll()).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.getAllThreatModels();

            assertThat(result).hasSize(2);
            verify(threatModelRepository).findAll();
        }

        @Test
        @DisplayName("Given no threat models exist, when getting all, should return empty list")
        void givenNoThreatModelsExist_whenGettingAll_shouldReturnEmptyList() {
            when(threatModelRepository.findAll()).thenReturn(List.of());

            List<ThreatModel> result = threatModelService.getAllThreatModels();

            assertThat(result).isEmpty();
            verify(threatModelRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Get Threat Model By Id Tests")
    class GetThreatModelByIdTests {

        @Test
        @DisplayName("Given threat model exists, when getting by id, should return threat model")
        void givenThreatModelExists_whenGettingById_shouldReturnThreatModel() {
            UUID modelId = UUID.randomUUID();
            ThreatModel expectedModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withId(modelId).build();
            when(threatModelRepository.findById(modelId)).thenReturn(Optional.of(expectedModel));

            Optional<ThreatModel> result = threatModelService.getThreatModelById(modelId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(modelId);
            verify(threatModelRepository).findById(modelId);
        }

        @Test
        @DisplayName("Given threat model does not exist, when getting by id, should return empty")
        void givenThreatModelDoesNotExist_whenGettingById_shouldReturnEmpty() {
            UUID modelId = UUID.randomUUID();
            when(threatModelRepository.findById(modelId)).thenReturn(Optional.empty());

            Optional<ThreatModel> result = threatModelService.getThreatModelById(modelId);

            assertThat(result).isEmpty();
            verify(threatModelRepository).findById(modelId);
        }
    }

    @Nested
    @DisplayName("Create Threat Model Tests")
    class CreateThreatModelTests {

        @Test
        @DisplayName("Given valid threat model with unique name, when creating, should save and return")
        void givenValidThreatModelWithUniqueName_whenCreating_shouldSaveAndReturn() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            when(threatModelRepository.existsByName(threatModel.getName())).thenReturn(false);
            when(threatModelRepository.save(any(ThreatModel.class))).thenReturn(threatModel);

            ThreatModel result = threatModelService.createThreatModel(threatModel);

            assertThat(result).isNotNull();
            verify(threatModelRepository).existsByName(threatModel.getName());
            verify(threatModelRepository).save(threatModel);
        }

        @Test
        @DisplayName("Given threat model with duplicate name, when creating, should throw exception")
        void givenThreatModelWithDuplicateName_whenCreating_shouldThrowException() {
            ThreatModel threatModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build();
            when(threatModelRepository.existsByName(threatModel.getName())).thenReturn(true);

            assertThatThrownBy(() -> threatModelService.createThreatModel(threatModel))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("A threat model with the name")
                    .hasMessageContaining("already exists");

            verify(threatModelRepository).existsByName(threatModel.getName());
            verify(threatModelRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Threat Model Tests")
    class UpdateThreatModelTests {

        @Test
        @DisplayName("Given existing threat model with new unique name, when updating, should update and return")
        void givenExistingThreatModelWithNewUniqueName_whenUpdating_shouldUpdateAndReturn() {
            UUID modelId = UUID.randomUUID();
            ThreatModel existingModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withId(modelId).build();
            ThreatModel updateData = ThreatModelTestDataBuilder.aWebApplicationThreatModel().build();
            ThreatModel updatedModel = ThreatModelTestDataBuilder.aWebApplicationThreatModel().withId(modelId).build();

            when(threatModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));
            when(threatModelRepository.existsByName(updateData.getName())).thenReturn(false);
            when(threatModelRepository.save(any(ThreatModel.class))).thenReturn(updatedModel);

            Optional<ThreatModel> result = threatModelService.updateThreatModel(modelId, updateData);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Web Application Threat Model");
            verify(threatModelRepository).findById(modelId);
            verify(threatModelRepository).save(any(ThreatModel.class));
        }

        @Test
        @DisplayName("Given non-existent threat model, when updating, should return empty")
        void givenNonExistentThreatModel_whenUpdating_shouldReturnEmpty() {
            UUID modelId = UUID.randomUUID();
            ThreatModel updateData = ThreatModelTestDataBuilder.aWebApplicationThreatModel().build();
            when(threatModelRepository.findById(modelId)).thenReturn(Optional.empty());

            Optional<ThreatModel> result = threatModelService.updateThreatModel(modelId, updateData);

            assertThat(result).isEmpty();
            verify(threatModelRepository).findById(modelId);
            verify(threatModelRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given update with duplicate name, when updating, should throw exception")
        void givenUpdateWithDuplicateName_whenUpdating_shouldThrowException() {
            UUID modelId = UUID.randomUUID();
            ThreatModel existingModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withId(modelId).build();
            ThreatModel updateData = ThreatModelTestDataBuilder.aWebApplicationThreatModel().build();

            when(threatModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));
            when(threatModelRepository.existsByName(updateData.getName())).thenReturn(true);

            assertThatThrownBy(() -> threatModelService.updateThreatModel(modelId, updateData))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("A threat model with the name")
                    .hasMessageContaining("already exists");

            verify(threatModelRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given update with same name, when updating, should not check for duplicates")
        void givenUpdateWithSameName_whenUpdating_shouldNotCheckForDuplicates() {
            UUID modelId = UUID.randomUUID();
            ThreatModel existingModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withId(modelId).build();
            ThreatModel updateData = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withDescription("Updated description").build();
            ThreatModel updatedModel = ThreatModelTestDataBuilder.aPaymentSystemThreatModel().withId(modelId).withDescription("Updated description").build();

            when(threatModelRepository.findById(modelId)).thenReturn(Optional.of(existingModel));
            when(threatModelRepository.save(any(ThreatModel.class))).thenReturn(updatedModel);

            Optional<ThreatModel> result = threatModelService.updateThreatModel(modelId, updateData);

            assertThat(result).isPresent();
            verify(threatModelRepository, never()).existsByName(any());
            verify(threatModelRepository).save(any(ThreatModel.class));
        }
    }

    @Nested
    @DisplayName("Delete Threat Model Tests")
    class DeleteThreatModelTests {

        @Test
        @DisplayName("Given existing threat model, when deleting, should delete and return true")
        void givenExistingThreatModel_whenDeleting_shouldDeleteAndReturnTrue() {
            UUID modelId = UUID.randomUUID();
            when(threatModelRepository.existsById(modelId)).thenReturn(true);

            boolean result = threatModelService.deleteThreatModel(modelId);

            assertThat(result).isTrue();
            verify(threatModelRepository).existsById(modelId);
            verify(threatModelRepository).deleteById(modelId);
        }

        @Test
        @DisplayName("Given non-existent threat model, when deleting, should return false")
        void givenNonExistentThreatModel_whenDeleting_shouldReturnFalse() {
            UUID modelId = UUID.randomUUID();
            when(threatModelRepository.existsById(modelId)).thenReturn(false);

            boolean result = threatModelService.deleteThreatModel(modelId);

            assertThat(result).isFalse();
            verify(threatModelRepository).existsById(modelId);
            verify(threatModelRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Get Threat Model Stats Tests")
    class GetThreatModelStatsTests {

        @Test
        @DisplayName("Given threat model with data, when getting stats, should return complete statistics")
        void givenThreatModelWithData_whenGettingStats_shouldReturnCompleteStatistics() {
            UUID modelId = UUID.randomUUID();
            when(componentRepository.countByThreatModelId(modelId)).thenReturn(5);
            when(vulnerabilityRepository.countByThreatModelIdAndStatusIn(eq(modelId), anyList())).thenReturn(8, 3);
            when(vulnerabilityRepository.countByThreatModelIdAndRiskScoreGreaterThanEqualAndStatusNotIn(
                    eq(modelId), eq(15), anyList())).thenReturn(2);

            ThreatModelStatsDTO stats = threatModelService.getThreatModelStats(modelId);

            assertThat(stats).isNotNull();
            assertThat(stats.getTotalComponents()).isEqualTo(5);
            assertThat(stats.getActiveThreats()).isEqualTo(8);
            assertThat(stats.getHighRiskThreats()).isEqualTo(2);
            assertThat(stats.getMitigatedThreats()).isEqualTo(3);
            verify(componentRepository).countByThreatModelId(modelId);
            verify(vulnerabilityRepository, times(2)).countByThreatModelIdAndStatusIn(eq(modelId), anyList());
            verify(vulnerabilityRepository).countByThreatModelIdAndRiskScoreGreaterThanEqualAndStatusNotIn(
                    eq(modelId), eq(15), anyList());
        }

        @Test
        @DisplayName("Given threat model with no data, when getting stats, should return zeros")
        void givenThreatModelWithNoData_whenGettingStats_shouldReturnZeros() {
            UUID modelId = UUID.randomUUID();
            when(componentRepository.countByThreatModelId(modelId)).thenReturn(0);
            when(vulnerabilityRepository.countByThreatModelIdAndStatusIn(eq(modelId), anyList())).thenReturn(0, 0);
            when(vulnerabilityRepository.countByThreatModelIdAndRiskScoreGreaterThanEqualAndStatusNotIn(
                    eq(modelId), eq(15), anyList())).thenReturn(0);

            ThreatModelStatsDTO stats = threatModelService.getThreatModelStats(modelId);

            assertThat(stats).isNotNull();
            assertThat(stats.getTotalComponents()).isZero();
            assertThat(stats.getActiveThreats()).isZero();
            assertThat(stats.getHighRiskThreats()).isZero();
            assertThat(stats.getMitigatedThreats()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Threats By Category Tests")
    class GetThreatsByCategoryTests {

        @Test
        @DisplayName("Given threat model with categorized threats, when getting by category, should return distribution")
        void givenThreatModelWithCategorizedThreats_whenGettingByCategory_shouldReturnDistribution() {
            UUID modelId = UUID.randomUUID();
            List<ThreatsByCategoryDTO> expectedData = List.of(
                    new ThreatsByCategoryDTO(StrideCategory.SPOOFING, 3L),
                    new ThreatsByCategoryDTO(StrideCategory.TAMPERING, 2L)
            );
            when(vulnerabilityRepository.countThreatsByCategory(modelId)).thenReturn(expectedData);

            List<ThreatsByCategoryDTO> result = threatModelService.getThreatsByCategory(modelId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCategory()).isEqualTo(StrideCategory.SPOOFING);
            assertThat(result.get(0).getCount()).isEqualTo(3L);
            verify(vulnerabilityRepository).countThreatsByCategory(modelId);
        }

        @Test
        @DisplayName("Given threat model with no threats, when getting by category, should return empty list")
        void givenThreatModelWithNoThreats_whenGettingByCategory_shouldReturnEmptyList() {
            UUID modelId = UUID.randomUUID();
            when(vulnerabilityRepository.countThreatsByCategory(modelId)).thenReturn(List.of());

            List<ThreatsByCategoryDTO> result = threatModelService.getThreatsByCategory(modelId);

            assertThat(result).isEmpty();
            verify(vulnerabilityRepository).countThreatsByCategory(modelId);
        }
    }

    @Nested
    @DisplayName("Get Risk Distribution Tests")
    class GetRiskDistributionTests {

        @Test
        @DisplayName("Given threat model with risk data, when getting distribution, should return risk levels")
        void givenThreatModelWithRiskData_whenGettingDistribution_shouldReturnRiskLevels() {
            UUID modelId = UUID.randomUUID();
            List<RiskDistributionDTO> expectedData = List.of(
                    new RiskDistributionDTO("Low", 5L),
                    new RiskDistributionDTO("Medium", 3L),
                    new RiskDistributionDTO("High", 2L)
            );
            when(vulnerabilityRepository.getRiskDistribution(modelId)).thenReturn(expectedData);

            List<RiskDistributionDTO> result = threatModelService.getRiskDistribution(modelId);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getRiskLevel()).isEqualTo("Low");
            assertThat(result.get(0).getCount()).isEqualTo(5L);
            verify(vulnerabilityRepository).getRiskDistribution(modelId);
        }

        @Test
        @DisplayName("Given threat model with no risks, when getting distribution, should return empty list")
        void givenThreatModelWithNoRisks_whenGettingDistribution_shouldReturnEmptyList() {
            UUID modelId = UUID.randomUUID();
            when(vulnerabilityRepository.getRiskDistribution(modelId)).thenReturn(List.of());

            List<RiskDistributionDTO> result = threatModelService.getRiskDistribution(modelId);

            assertThat(result).isEmpty();
            verify(vulnerabilityRepository).getRiskDistribution(modelId);
        }
    }

    @Nested
    @DisplayName("Search Threat Models Tests")
    class SearchThreatModelsTests {

        @Test
        @DisplayName("Given null search term and ALL filter, when searching, should return all models")
        void givenNullSearchAndAllFilter_whenSearching_shouldReturnAllModels() {
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build(),
                    ThreatModelTestDataBuilder.aWebApplicationThreatModel().build()
            );
            when(threatModelRepository.findAll()).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(null, ThreatModelFilter.ALL);

            assertThat(result).hasSize(2);
            verify(threatModelRepository).findAll();
            verify(threatModelRepository, never()).searchThreatModels(any());
        }

        @Test
        @DisplayName("Given empty search term and ALL filter, when searching, should return all models")
        void givenEmptySearchAndAllFilter_whenSearching_shouldReturnAllModels() {
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findAll()).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels("   ", ThreatModelFilter.ALL);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findAll();
        }

        @Test
        @DisplayName("Given search term and ALL filter, when searching, should call searchThreatModels")
        void givenSearchTermAndAllFilter_whenSearching_shouldCallSearchThreatModels() {
            String searchTerm = "Payment";
            String searchPattern = "%payment%";
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.searchThreatModels(searchPattern)).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).searchThreatModels(searchPattern);
            verify(threatModelRepository, never()).findAll();
        }

        @Test
        @DisplayName("Given ACTIVE_THREATS filter without search, when searching, should call findThreatModelsWithActiveThreats")
        void givenActiveThreatsFilterWithoutSearch_whenSearching_shouldCallCorrectMethod() {
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findThreatModelsWithActiveThreats(isNull())).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(null, ThreatModelFilter.ACTIVE_THREATS);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findThreatModelsWithActiveThreats(isNull());
        }

        @Test
        @DisplayName("Given ACTIVE_THREATS filter with search, when searching, should pass search term")
        void givenActiveThreatsFilterWithSearch_whenSearching_shouldPassSearchTerm() {
            String searchTerm = "Payment";
            String searchPattern = "%payment%";
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findThreatModelsWithActiveThreats(searchPattern)).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ACTIVE_THREATS);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findThreatModelsWithActiveThreats(searchPattern);
        }

        @Test
        @DisplayName("Given HIGH_RISK_THREATS filter without search, when searching, should call findThreatModelsWithHighRiskActiveThreats")
        void givenHighRiskThreatsFilterWithoutSearch_whenSearching_shouldCallCorrectMethod() {
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findThreatModelsWithHighRiskActiveThreats(isNull(), anyInt())).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(null, ThreatModelFilter.HIGH_RISK_THREATS);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findThreatModelsWithHighRiskActiveThreats(isNull(), eq(16));
        }

        @Test
        @DisplayName("Given HIGH_RISK_THREATS filter with search, when searching, should pass search term and threshold")
        void givenHighRiskThreatsFilterWithSearch_whenSearching_shouldPassSearchTermAndThreshold() {
            String searchTerm = "Payment";
            String searchPattern = "%payment%";
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findThreatModelsWithHighRiskActiveThreats(searchPattern, 16)).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.HIGH_RISK_THREATS);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findThreatModelsWithHighRiskActiveThreats(searchPattern, 16);
        }

        @Test
        @DisplayName("Given MITIGATED_THREATS filter without search, when searching, should call findThreatModelsWithMitigatedThreats")
        void givenMitigatedThreatsFilterWithoutSearch_whenSearching_shouldCallCorrectMethod() {
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findThreatModelsWithMitigatedThreats(isNull())).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(null, ThreatModelFilter.MITIGATED_THREATS);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findThreatModelsWithMitigatedThreats(isNull());
        }

        @Test
        @DisplayName("Given MITIGATED_THREATS filter with search, when searching, should pass search term")
        void givenMitigatedThreatsFilterWithSearch_whenSearching_shouldPassSearchTerm() {
            String searchTerm = "Payment";
            String searchPattern = "%payment%";
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.findThreatModelsWithMitigatedThreats(searchPattern)).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.MITIGATED_THREATS);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).findThreatModelsWithMitigatedThreats(searchPattern);
        }

        @Test
        @DisplayName("Given search with leading/trailing whitespace, when searching, should trim search term")
        void givenSearchWithWhitespace_whenSearching_shouldTrimSearchTerm() {
            String searchTerm = "  Payment  ";
            String searchPattern = "%payment%";
            List<ThreatModel> expectedModels = List.of(
                    ThreatModelTestDataBuilder.aPaymentSystemThreatModel().build()
            );
            when(threatModelRepository.searchThreatModels(searchPattern)).thenReturn(expectedModels);

            List<ThreatModel> result = threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL);

            assertThat(result).hasSize(1);
            verify(threatModelRepository).searchThreatModels(searchPattern);
        }

        @Test
        @DisplayName("Given search with no results, when searching, should return empty list")
        void givenSearchWithNoResults_whenSearching_shouldReturnEmptyList() {
            String searchTerm = "NonExistent";
            String searchPattern = "%nonexistent%";
            when(threatModelRepository.searchThreatModels(searchPattern)).thenReturn(List.of());

            List<ThreatModel> result = threatModelService.searchThreatModels(searchTerm, ThreatModelFilter.ALL);

            assertThat(result).isEmpty();
            verify(threatModelRepository).searchThreatModels(searchPattern);
        }
    }
}
