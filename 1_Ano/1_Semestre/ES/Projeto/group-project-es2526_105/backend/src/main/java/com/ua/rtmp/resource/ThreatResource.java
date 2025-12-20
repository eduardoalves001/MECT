package com.ua.rtmp.resource;

import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.model.Threat;
import com.ua.rtmp.service.ThreatService;
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
    name = "Threat Resource",
    description = "Endpoints for managing threats. " +
                  "Provides operations to list and retrieve threats. " +
                  "Each threat contains information about a specific security risk."
)
@RestController
@RequestMapping("/api/v1/threats")
@RequiredArgsConstructor
public class ThreatResource {

    private final ThreatService threatService;

    @Operation(
        summary = "Get all threats",
        description = "Retrieves a list of all threats in the system. " +
                      "Returns detailed information for each threat, including its name and description."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved list of threats"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled")
    })
    @PreAuthorize("hasRole('threat:read')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Threat>>> getAllThreats() {
        log.debug("Fetching all threats");
        List<Threat> list = threatService.getAllThreats();
        log.info("Retrieved {} threats", list.size());
        return ResponseEntity.ok(ApiResponse.success("Threats retrieved successfully", list));
    }

    @Operation(
        summary = "Get a specific threat",
        description = "Retrieves details of a specific threat by its ID. " +
                      "Returns the threat's name, description, and other relevant information."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved threat"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Feature temporarily disabled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Threat not found")
    })
    @PreAuthorize("hasRole('threat:read')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Threat>> getThreatById(
            @Parameter(description = "UUID of the threat", required = true)
            @PathVariable UUID id) {
        log.debug("Fetching threat: id={}", id);
        return threatService.getThreatById(id)
                .map(threat -> {
                    log.info("Retrieved threat: id={}, name={}", id, threat.getName());
                    return ResponseEntity.ok(ApiResponse.success("Threat retrieved successfully", threat));
                })
                .orElseGet(() -> {
                    log.warn("Threat not found: id={}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Threat not found"));
                });
    }

}