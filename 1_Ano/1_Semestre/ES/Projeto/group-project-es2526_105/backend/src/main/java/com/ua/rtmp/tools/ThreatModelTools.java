package com.ua.rtmp.tools;

import com.ua.rtmp.dto.response.ThreatModelStatsDTO;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.enums.ThreatModelFilter;
import com.ua.rtmp.service.ThreatModelService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatModelTools {

    private final ThreatModelService threatModelService;

    @Tool("Retrieves the user's threat models with optional filtering. " +
          "Returns a list of threat models matching the search criteria and filter. " +
          "Use this when user asks about their threat models, risk assessments, or security posture.")
    public List<String> getUserThreatModels(
            @P("Optional search term to filter threat models by name or description") String search,
            @P("Filter type: ALL (all models), HIGH_RISK_THREATS (models with high-risk threats), " +
               "ACTIVE_THREATS (models with active threats), MITIGATED_THREATS (models with mitigated threats)")
            String filter) {

        log.info("ThreatModelTools.getUserThreatModels called: search={}, filter={}", search, filter);

        try {
            ThreatModelFilter filterEnum = filter != null && !filter.trim().isEmpty()
                ? ThreatModelFilter.valueOf(filter.toUpperCase().trim())
                : ThreatModelFilter.ALL;

            List<ThreatModel> models = threatModelService.searchThreatModels(
                search != null && !search.trim().isEmpty() ? search.trim() : null,
                filterEnum
            );

            log.debug("Found {} threat models", models.size());

            if (models.isEmpty()) {
                return List.of("No threat models found matching the criteria.");
            }

            return models.stream()
                .map(this::formatModelSummary)
                .collect(Collectors.toList());

        } catch (IllegalArgumentException e) {
            log.error("Invalid filter value: {}", filter, e);
            return List.of("Error: Invalid filter value. Use: ALL, HIGH_RISK_THREATS, ACTIVE_THREATS, or MITIGATED_THREATS");
        }
    }

    @Tool("Gets detailed statistics for a specific threat model. " +
          "Returns information about total components, active threats, high-risk threats, and mitigated threats. " +
          "Use this when user asks for metrics, statistics, or detailed information about a specific model.")
    public String getThreatModelStats(@P("The UUID of the threat model") String id) {
        log.info("ThreatModelTools.getThreatModelStats called: id={}", id);

        try {
            UUID modelId = UUID.fromString(id);
            ThreatModelStatsDTO stats = threatModelService.getThreatModelStats(modelId);

            return formatStats(stats);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id, e);
            return "Error: Invalid threat model ID format";
        } catch (Exception e) {
            log.error("Error getting threat model stats: id={}", id, e);
            return "Error: Could not retrieve threat model statistics";
        }
    }

    private String formatModelSummary(ThreatModel model) {
        return String.format(
            "ID: %s | Name: %s | Description: %s | Created: %s",
            model.getId(),
            model.getName(),
            model.getDescription() != null ? model.getDescription() : "No description",
            model.getCreatedAt()
        );
    }

    private String formatStats(ThreatModelStatsDTO stats) {
        return String.format(
            "Statistics:\n" +
            "- Total Components: %d\n" +
            "- Active Threats: %d\n" +
            "- High Risk Threats: %d\n" +
            "- Mitigated Threats: %d",
            stats.getTotalComponents(),
            stats.getActiveThreats(),
            stats.getHighRiskThreats(),
            stats.getMitigatedThreats()
        );
    }
}
