package com.ua.rtmp.resource;

import com.ua.rtmp.annotation.RequireFeatureFlag;
import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.dto.response.RiskDistributionDTO;
import com.ua.rtmp.dto.response.ThreatModelStatsDTO;
import com.ua.rtmp.dto.response.ThreatsByCategoryDTO;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.enums.ThreatModelFilter;
import com.ua.rtmp.service.ThreatModelService;
import com.ua.rtmp.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(
    name = "Threat Model Resource",
    description = "Endpoints for managing threat models. " +
                  "Provides operations to list, retrieve, create, update, and delete threat models. " +
                  "Each threat model contains information about potential security threats and their mitigations."
)
@RestController
@RequestMapping("/api/v1/threat-models")
@RequiredArgsConstructor
public class ThreatModelResource {

    private final ThreatModelService threatModelService;
    private final ExportService exportService;

    @Operation(
        summary = "Get all threat models",
        description = "Retrieves a list of all threat models in the system. " +
                      "Returns detailed information for each threat model, including its name and description. " +
                      "Supports optional search parameter to filter threat models by name or description, " +
                      "and filter parameter to show models with specific threat conditions."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved list of threat models")
    })
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ThreatModel>>> getAllThreatModels(
            @Parameter(description = "Search term to filter threat models by name or description", required = false)
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter to show models with specific threat conditions (ALL, ACTIVE_THREATS, HIGH_RISK_THREATS, MITIGATED_THREATS)", required = false)
            @RequestParam(required = false, defaultValue = "ALL") ThreatModelFilter filter) {
        log.debug("Fetching all threat models with search={}, filter={}", search, filter);
        List<ThreatModel> threatModels = threatModelService.searchThreatModels(search, filter);
        log.info("Retrieved {} threat models", threatModels.size());
        return ResponseEntity.ok(ApiResponse.success("Threat models retrieved successfully", threatModels));
    }

    @Operation(
        summary = "Get a specific threat model",
        description = "Retrieves details of a specific threat model by its ID. " +
                      "Returns the threat model's name, description, and other relevant information."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved threat model"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreatModel>> getThreatModelById(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Fetching threat model with id={}", id);
        return threatModelService.getThreatModelById(id)
                .map(threatModel -> {
                    log.info("Retrieved threat model: id={}, name={}", id, threatModel.getName());
                    return ResponseEntity.ok(ApiResponse.success("Threat model retrieved successfully", threatModel));
                })
                .orElseGet(() -> {
                    log.warn("Threat model not found: id={}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Threat model not found"));
                });
    }

    @Operation(
        summary = "Create a new threat model",
        description = "Creates a new threat model. " +
                      "Requires the threat model's name and description in the request body. " +
                      "Returns the created threat model with its assigned ID."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Threat model created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled")
    })
    @PreAuthorize("hasRole('threatmodel:create')")
    @PostMapping
    public ResponseEntity<ApiResponse<ThreatModel>> createThreatModel(
            @Valid @RequestBody ThreatModel threatModel) {
        log.debug("Creating threat model: name={}", threatModel.getName());
        ThreatModel createdThreatModel = threatModelService.createThreatModel(threatModel);
        log.info("Created threat model: id={}, name={}", createdThreatModel.getId(), createdThreatModel.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Threat model created successfully", createdThreatModel));
    }

    @Operation(
        summary = "Update an existing threat model",
        description = "Updates the name and description of an existing threat model. " +
                      "Requires the updated name and description in the request body. " +
                      "Returns the updated threat model details."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Threat model updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('threatmodel:update')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreatModel>> updateThreatModel(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ThreatModel updateData) {
        log.debug("Updating threat model: id={}, name={}", id, updateData.getName());
        return threatModelService.updateThreatModel(id, updateData)
                .map(threatModel -> {
                    log.info("Updated threat model: id={}, name={}", id, threatModel.getName());
                    return ResponseEntity.ok(ApiResponse.success("Threat model updated successfully", threatModel));
                })
                .orElseGet(() -> {
                    log.warn("Threat model not found for update: id={}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Threat model not found"));
                });
    }

    @Operation(
        summary = "Delete a threat model",
        description = "Deletes a specific threat model by its ID. " +
                      "Removes the threat model and returns a success message if deletion is successful."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Threat model deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('threatmodel:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThreatModel(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Deleting threat model: id={}", id);
        if (threatModelService.deleteThreatModel(id)) {
            log.info("Deleted threat model: id={}", id);
            return ResponseEntity.ok(ApiResponse.success("Threat model deleted successfully", null));
        }
        log.warn("Threat model not found for deletion: id={}", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Threat model not found"));
    }

    @Operation(
        summary = "Get threat model statistics",
        description = "Retrieves statistical summary for a specific threat model including total components, active threats, high risk threats, and mitigated threats."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<ThreatModelStatsDTO>> getThreatModelStats(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Fetching threat model statistics: id={}", id);
        ThreatModelStatsDTO stats = threatModelService.getThreatModelStats(id);
        log.info("Retrieved threat model statistics: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("Threat model statistics retrieved successfully", stats));
    }

    @Operation(
        summary = "Get threats by category",
        description = "Retrieves the count of threats grouped by STRIDE category for a specific threat model."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Threats by category retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping("/{id}/threats-by-category")
    public ResponseEntity<ApiResponse<List<ThreatsByCategoryDTO>>> getThreatsByCategory(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Fetching threats by category: threatModelId={}", id);
        List<ThreatsByCategoryDTO> data = threatModelService.getThreatsByCategory(id);
        log.info("Retrieved threats by category: threatModelId={}, categories={}", id, data.size());
        return ResponseEntity.ok(ApiResponse.success("Threats by category retrieved successfully", data));
    }

    @Operation(
        summary = "Get risk distribution",
        description = "Retrieves the distribution of vulnerabilities by risk level (Critical, High, Medium, Low) for a specific threat model."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Risk distribution retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found")
    })
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping("/{id}/risk-distribution")
    public ResponseEntity<ApiResponse<List<RiskDistributionDTO>>> getRiskDistribution(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Fetching risk distribution: threatModelId={}", id);
        List<RiskDistributionDTO> data = threatModelService.getRiskDistribution(id);
        log.info("Retrieved risk distribution: threatModelId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Risk distribution retrieved successfully", data));
    }

    @Operation(
        summary = "Export threat model to PDF",
        description = "Exports the threat model with all components, threats, risk scores, and mitigation strategies to a PDF document. " +
                      "The PDF includes project metadata, component details, and a formatted table of all identified threats."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error generating PDF")
    })
    @RequireFeatureFlag("enable_export_features")
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping(value = "/{id}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportToPdf(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Exporting threat model to PDF: id={}", id);
        try {
            byte[] pdfData = exportService.exportToPdf(id);
            log.info("Successfully exported threat model to PDF: id={}, size={} bytes", id, pdfData.length);
            return ResponseEntity.ok(pdfData);
        } catch (IOException e) {
            log.error("Error exporting threat model to PDF: id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Export threat model to CSV",
        description = "Exports the threat model with all components, threats, risk scores, and mitigation strategies to a CSV file. " +
                      "The CSV includes project metadata, component details, and detailed information for each identified threat."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat model not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error generating CSV")
    })
    @RequireFeatureFlag("enable_export_features")
    @PreAuthorize("hasRole('threatmodel:read')")
    @GetMapping(value = "/{id}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportToCsv(
            @Parameter(description = "UUID of the threat model", required = true)
            @PathVariable UUID id) {
        log.debug("Exporting threat model to CSV: id={}", id);
        try {
            String csvData = exportService.exportToCsv(id);
            log.info("Successfully exported threat model to CSV: id={}, size={} bytes", id, csvData.length());
            return ResponseEntity.ok(csvData);
        } catch (IOException e) {
            log.error("Error exporting threat model to CSV: id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}