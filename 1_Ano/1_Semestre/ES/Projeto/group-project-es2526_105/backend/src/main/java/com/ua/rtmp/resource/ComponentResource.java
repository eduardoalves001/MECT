package com.ua.rtmp.resource;

import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.model.Component;
import com.ua.rtmp.service.ComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(
    name = "Component Resource",
    description = "Endpoints for managing components within a threat model. " +
                  "Provides operations to list, retrieve, create, update, and delete components. " +
                  "Each component represents a part of the threat model and contains details such as name and description."
)
@RestController
@RequestMapping("/api/v1/threat-models/{threatModelId}/components")
@RequiredArgsConstructor
public class ComponentResource {

    private final ComponentService componentService;

    @Operation(
        summary = "Get all components for a threat model",
        description = "Retrieves a list of all components associated with the specified threat model. " +
                      "Returns detailed information for each component, including its name and description. " +
                      "Supports optional search parameter to filter components by name or description."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved list of components"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('component:read')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Component>>> getAllComponents(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID threatModelId,
            @Parameter(description = "Search term to filter components by name or description", required = false)
            @RequestParam(required = false) String search) {
        log.debug("Fetching all components for threat model: threatModelId={}, search={}", threatModelId, search);
        List<Component> components = componentService.searchComponents(threatModelId, search);
        log.info("Retrieved {} components for threat model: threatModelId={}", components.size(), threatModelId);
        return ResponseEntity.ok(ApiResponse.success("Components retrieved successfully", components));
    }

    @Operation(
        summary = "Get a specific component",
        description = "Retrieves details of a specific component by its ID within the given threat model. " +
                      "Returns the component's name, description, and other relevant information."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved component"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Component or threat model not found")
    })
    @PreAuthorize("hasRole('component:read')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Component>> getComponent(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID threatModelId,
            @Parameter(description = "UUID of the component", required = true)
            @PathVariable UUID id) {
        log.debug("Fetching component: id={}, threatModelId={}", id, threatModelId);
        Component component = componentService.getComponentById(id);
        log.info("Retrieved component: id={}, name={}", id, component.getName());
        return ResponseEntity.ok(ApiResponse.success("Component retrieved successfully", component));
    }

    @Operation(
        summary = "Create a new component",
        description = "Creates a new component within the specified threat model. " +
                      "Requires the component's name and description in the request body. " +
                      "Returns the created component with its assigned ID."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Component created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('component:create')")
    @PostMapping
    public ResponseEntity<ApiResponse<Component>> createComponent(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID threatModelId,
            @Valid @RequestBody Component component) {
        log.debug("Creating component: name={}, threatModelId={}", component.getName(), threatModelId);
        Component createdComponent = componentService.createComponent(threatModelId, component.getName(), component.getDescription());
        log.info("Created component: id={}, name={}, threatModelId={}", createdComponent.getId(), createdComponent.getName(), threatModelId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Component created successfully", createdComponent));
    }

    @Operation(
        summary = "Update an existing component",
        description = "Updates the name and description of an existing component within the specified threat model. " +
                      "Requires the updated name and description in the request body. " +
                      "Returns the updated component details."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Component updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Component or threat model not found")
    })
    @PreAuthorize("hasRole('component:update')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Component>> updateComponent(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID threatModelId,
            @Parameter(description = "UUID of the component", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody Component updateData) {
        log.debug("Updating component: id={}, name={}, threatModelId={}", id, updateData.getName(), threatModelId);
        Component updatedComponent = componentService.updateComponent(id, updateData.getName(), updateData.getDescription());
        log.info("Updated component: id={}, name={}", id, updatedComponent.getName());
        return ResponseEntity.ok(ApiResponse.success("Component updated successfully", updatedComponent));
    }

    @Operation(
        summary = "Delete a component",
        description = "Deletes a specific component from the specified threat model. " +
                      "Removes the component and returns a success message if deletion is successful."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Component deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Component or threat model not found")
    })
    @PreAuthorize("hasRole('component:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComponent(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID threatModelId,
            @Parameter(description = "UUID of the component", required = true)
            @PathVariable UUID id) {
        log.debug("Deleting component: id={}, threatModelId={}", id, threatModelId);
        componentService.deleteComponent(id);
        log.info("Deleted component: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("Component deleted successfully", null));
    }
}