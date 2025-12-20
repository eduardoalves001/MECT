package com.ua.rtmp.service;

import com.ua.rtmp.dto.response.RiskDistributionDTO;
import com.ua.rtmp.dto.response.ThreatModelStatsDTO;
import com.ua.rtmp.dto.response.ThreatsByCategoryDTO;
import com.ua.rtmp.exception.DuplicateResourceException;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.enums.ThreatModelFilter;
import com.ua.rtmp.model.enums.VulnerabilityStatus;
import com.ua.rtmp.repository.ComponentRepository;
import com.ua.rtmp.repository.ThreatModelRepository;
import com.ua.rtmp.repository.VulnerabilityRepository;
import com.ua.rtmp.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ThreatModelService {

    private static final int HIGH_RISK_THRESHOLD = 16; // Risk score >= 16 is considered high risk (4x4 or 5x3.2+)

    private final ThreatModelRepository threatModelRepository;
    private final ComponentRepository componentRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    @Transactional(readOnly = true)
    public List<ThreatModel> getAllThreatModels() {
        log.info("getAllThreatModels started");
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", null);
        
        List<ThreatModel> models = threatModelRepository.findAll();
        log.info("getAllThreatModels completed successfully: count={}", models.size());
        return models;
    }

    @Transactional(readOnly = true)
    public List<ThreatModel> searchThreatModels(String search) {
        log.info("searchThreatModels started: search={}", search);
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", null);
        
        if (search == null || search.trim().isEmpty()) {
            log.debug("Empty search parameter, returning all threat models");
            List<ThreatModel> models = threatModelRepository.findAll();
            log.info("searchThreatModels completed successfully: count={}", models.size());
            return models;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        log.debug("Searching with pattern: {}", pattern);
        
        List<ThreatModel> models = threatModelRepository.searchThreatModels(pattern);
        log.info("searchThreatModels completed successfully: search={}, count={}", search, models.size());
        return models;
    }

    @Transactional(readOnly = true)
    public List<ThreatModel> searchThreatModels(String search, ThreatModelFilter filter) {
        log.info("searchThreatModels started: search={}, filter={}", search, filter);
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", null);
        
        String trimmedSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String searchPattern = trimmedSearch != null ? "%" + trimmedSearch.toLowerCase() + "%" : null;
        
        log.debug("Processing filter: {}, searchPattern: {}", filter, searchPattern);
        List<ThreatModel> models;
        
        switch (filter) {
            case ACTIVE_THREATS:
                models = threatModelRepository.findThreatModelsWithActiveThreats(searchPattern);
                break;
            case HIGH_RISK_THREATS:
                models = threatModelRepository.findThreatModelsWithHighRiskActiveThreats(searchPattern, HIGH_RISK_THRESHOLD);
                break;
            case MITIGATED_THREATS:
                models = threatModelRepository.findThreatModelsWithMitigatedThreats(searchPattern);
                break;
            case ALL:
            default:
                if (trimmedSearch == null) {
                    models = threatModelRepository.findAll();
                } else {
                    models = threatModelRepository.searchThreatModels(searchPattern);
                }
                break;
        }
        
        log.info("searchThreatModels completed successfully: search={}, filter={}, count={}", search, filter, models.size());
        return models;
    }

    @Transactional(readOnly = true)
    public Optional<ThreatModel> getThreatModelById(UUID id) {
        log.info("getThreatModelById started: id={}", id);
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", id.toString());
        
        Optional<ThreatModel> model = threatModelRepository.findById(id);
        if (model.isPresent()) {
            log.info("getThreatModelById completed successfully: id={}, name={}", id, model.get().getName());
        } else {
            log.warn("getThreatModelById: threat model not found: id={}", id);
        }
        return model;
    }

    public ThreatModel createThreatModel(ThreatModel threatModel) {
        log.info("createThreatModel started: name={}", threatModel.getName());
        LoggingUtil.setOperationContext("CREATE", "THREAT_MODEL", null);
        
        if (threatModelRepository.existsByName(threatModel.getName())) {
            log.error("createThreatModel failed: duplicate name: name={}", threatModel.getName());
            throw new DuplicateResourceException("A threat model with the name '" + threatModel.getName() + "' already exists. Please choose a different name.");
        }
        
        ThreatModel saved = threatModelRepository.save(threatModel);
        if (saved.getId() != null) {
            LoggingUtil.setOperationContext("CREATE", "THREAT_MODEL", saved.getId().toString());
        }
        log.info("createThreatModel completed successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Optional<ThreatModel> updateThreatModel(UUID id, ThreatModel updateData) {
        log.info("updateThreatModel started: id={}, newName={}", id, updateData.getName());
        LoggingUtil.setOperationContext("UPDATE", "THREAT_MODEL", id.toString());
        
        Optional<ThreatModel> result = threatModelRepository.findById(id)
                .map(existingThreatModel -> {
                    log.debug("Found existing threat model: id={}, oldName={}", id, existingThreatModel.getName());
                    
                    if (!existingThreatModel.getName().equals(updateData.getName()) &&
                        threatModelRepository.existsByName(updateData.getName())) {
                        log.error("updateThreatModel failed: duplicate name: id={}, newName={}", id, updateData.getName());
                        throw new DuplicateResourceException("A threat model with the name '" + updateData.getName() + "' already exists. Please choose a different name.");
                    }

                    existingThreatModel.setName(updateData.getName());
                    existingThreatModel.setDescription(updateData.getDescription());
                    ThreatModel saved = threatModelRepository.save(existingThreatModel);
                    log.info("updateThreatModel completed successfully: id={}, name={}", saved.getId(), saved.getName());
                    return saved;
                });
        
        if (result.isEmpty()) {
            log.warn("updateThreatModel: threat model not found: id={}", id);
        }
        return result;
    }

    public boolean deleteThreatModel(UUID id) {
        log.info("deleteThreatModel started: id={}", id);
        LoggingUtil.setOperationContext("DELETE", "THREAT_MODEL", id.toString());
        
        if (threatModelRepository.existsById(id)) {
            threatModelRepository.deleteById(id);
            log.info("deleteThreatModel completed successfully: id={}", id);
            return true;
        }
        log.warn("deleteThreatModel: threat model not found: id={}", id);
        return false;
    }

    @Transactional(readOnly = true)
    public ThreatModelStatsDTO getThreatModelStats(UUID threatModelId) {
        log.info("getThreatModelStats started: threatModelId={}", threatModelId);
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", threatModelId.toString());
        
        // Total components
        Integer totalComponents = componentRepository.countByThreatModelId(threatModelId);
        log.debug("Total components count: {}", totalComponents);
        
        // Active threats (IDENTIFIED, ANALYSED, IN_PROGRESS)
        Integer activeThreats = vulnerabilityRepository.countByThreatModelIdAndStatusIn(
            threatModelId, 
            List.of(VulnerabilityStatus.IDENTIFIED, VulnerabilityStatus.ANALYSED, VulnerabilityStatus.IN_PROGRESS)
        );
        log.debug("Active threats count: {}", activeThreats);
        
        // High risk threats (risk score >= 15, excluding MITIGATED and CLOSED)
        Integer highRiskThreats = vulnerabilityRepository.countByThreatModelIdAndRiskScoreGreaterThanEqualAndStatusNotIn(
            threatModelId, 
            15,
            List.of(VulnerabilityStatus.MITIGATED, VulnerabilityStatus.CLOSED)
        );
        log.debug("High risk threats count: {}", highRiskThreats);
        
        // Mitigated threats (MITIGATED, CLOSED)
        Integer mitigatedThreats = vulnerabilityRepository.countByThreatModelIdAndStatusIn(
            threatModelId,
            List.of(VulnerabilityStatus.MITIGATED, VulnerabilityStatus.CLOSED)
        );
        log.debug("Mitigated threats count: {}", mitigatedThreats);
        
        ThreatModelStatsDTO stats = new ThreatModelStatsDTO(totalComponents, activeThreats, highRiskThreats, mitigatedThreats);
        log.info("getThreatModelStats completed successfully: threatModelId={}, totalComponents={}, activeThreats={}, highRiskThreats={}, mitigatedThreats={}",
                threatModelId, totalComponents, activeThreats, highRiskThreats, mitigatedThreats);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<ThreatsByCategoryDTO> getThreatsByCategory(UUID threatModelId) {
        log.info("getThreatsByCategory started: threatModelId={}", threatModelId);
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", threatModelId.toString());
        
        List<ThreatsByCategoryDTO> categories = vulnerabilityRepository.countThreatsByCategory(threatModelId);
        log.info("getThreatsByCategory completed successfully: threatModelId={}, categoriesCount={}", threatModelId, categories.size());
        return categories;
    }

    @Transactional(readOnly = true)
    public List<RiskDistributionDTO> getRiskDistribution(UUID threatModelId) {
        log.info("getRiskDistribution started: threatModelId={}", threatModelId);
        LoggingUtil.setOperationContext("READ", "THREAT_MODEL", threatModelId.toString());
        
        List<RiskDistributionDTO> distribution = vulnerabilityRepository.getRiskDistribution(threatModelId);
        log.info("getRiskDistribution completed successfully: threatModelId={}, riskLevelsCount={}", threatModelId, distribution.size());
        return distribution;
    }
}
