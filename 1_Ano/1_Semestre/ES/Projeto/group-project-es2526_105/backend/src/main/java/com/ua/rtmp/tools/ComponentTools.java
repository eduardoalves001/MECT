package com.ua.rtmp.tools;

import com.ua.rtmp.model.Component;
import com.ua.rtmp.service.ComponentService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class ComponentTools {

    private final ComponentService componentService;

    @Tool("Lists all components in a specific threat model. " +
          "Returns basic information about each component including ID, name, and description. " +
          "Use this when user asks about components, system architecture, or what parts make up a threat model.")
    public List<String> getModelComponents(@P("The UUID of the threat model") String modelId) {
        log.info("ComponentTools.getModelComponents called: modelId={}", modelId);

        try {
            UUID threatModelId = UUID.fromString(modelId);
            List<Component> components = componentService.getAllComponentsByThreatModelId(threatModelId);

            log.debug("Found {} components for threat model {}", components.size(), modelId);

            if (components.isEmpty()) {
                return List.of("No components found for this threat model.");
            }

            return components.stream()
                .map(this::formatComponentSummary)
                .collect(Collectors.toList());

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", modelId, e);
            return List.of("Error: Invalid threat model ID format");
        } catch (Exception e) {
            log.error("Error getting components for threat model: modelId={}", modelId, e);
            return List.of("Error: Could not retrieve components");
        }
    }

    @Tool("Gets detailed information about a specific component. " +
          "Returns comprehensive details including ID, name, description, and parent threat model. " +
          "Use this when user asks for details about a particular component or system part.")
    public String getComponentDetails(@P("The UUID of the component") String componentId) {
        log.info("ComponentTools.getComponentDetails called: componentId={}", componentId);

        try {
            UUID id = UUID.fromString(componentId);
            Component component = componentService.getComponentById(id);

            return formatComponentDetails(component);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", componentId, e);
            return "Error: Invalid component ID format";
        } catch (Exception e) {
            log.error("Error getting component details: componentId={}", componentId, e);
            return "Error: Could not retrieve component details. Component may not exist.";
        }
    }

    private String formatComponentSummary(Component component) {
        return String.format(
            "ID: %s | Name: %s | Description: %s",
            component.getId(),
            component.getName(),
            component.getDescription() != null ? component.getDescription() : "No description"
        );
    }

    private String formatComponentDetails(Component component) {
        return String.format(
            "Component Details:\n" +
            "- ID: %s\n" +
            "- Name: %s\n" +
            "- Description: %s\n" +
            "- Threat Model: %s (ID: %s)",
            component.getId(),
            component.getName(),
            component.getDescription() != null ? component.getDescription() : "No description",
            component.getThreatModel().getName(),
            component.getThreatModel().getId()
        );
    }
}
