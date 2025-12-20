package com.ua.rtmp.tools;

import com.ua.rtmp.model.Threat;
import com.ua.rtmp.service.ThreatService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatTools {

    private final ThreatService threatService;

    @Tool("Lists all available threats from the STRIDE threat library. " +
          "Returns information about threat names, categories, and descriptions. " +
          "Use this when user asks about available threats, STRIDE categories, or what threats can be added to components.")
    public List<String> getAllThreats() {
        log.info("ThreatTools.getAllThreats called");

        try {
            List<Threat> threats = threatService.getAllThreats();

            log.debug("Found {} threats in library", threats.size());

            if (threats.isEmpty()) {
                return List.of("No threats found in the library.");
            }

            return threats.stream()
                .map(this::formatThreatSummary)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting all threats", e);
            return List.of("Error: Could not retrieve threats from library");
        }
    }

    @Tool("Gets detailed information about a specific threat from the library. " +
          "Returns threat name, STRIDE category, and comprehensive description. " +
          "Use this when user asks for details about a particular threat type.")
    public String getThreatDetails(@P("The UUID of the threat") String id) {
        log.info("ThreatTools.getThreatDetails called: id={}", id);

        try {
            UUID threatId = UUID.fromString(id);
            Optional<Threat> threat = threatService.getThreatById(threatId);

            if (threat.isEmpty()) {
                return "Error: Threat not found in the library";
            }

            return formatThreatDetails(threat.get());

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id, e);
            return "Error: Invalid threat ID format";
        } catch (Exception e) {
            log.error("Error getting threat details: id={}", id, e);
            return "Error: Could not retrieve threat details";
        }
    }

    private String formatThreatSummary(Threat threat) {
        return String.format(
            "Name: %s | Category: %s | Description: %s",
            threat.getName(),
            threat.getCategory() != null ? threat.getCategory().name() : "Not specified",
            threat.getDescription() != null && threat.getDescription().length() > 80
                ? threat.getDescription().substring(0, 77) + "..."
                : (threat.getDescription() != null ? threat.getDescription() : "No description")
        );
    }

    private String formatThreatDetails(Threat threat) {
        return String.format(
            "Threat Details:\n" +
            "- Name: %s\n" +
            "- STRIDE Category: %s\n" +
            "- Description: %s",
            threat.getName(),
            threat.getCategory() != null ? threat.getCategory().name() : "Not specified",
            threat.getDescription() != null ? threat.getDescription() : "No description"
        );
    }
}
