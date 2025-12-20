package com.ua.rtmp.resource;

import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(
    name = "Feature Flag Resource",
    description = "Endpoints for checking feature flag status. " +
                  "Provides operations to check if specific features are enabled dynamically."
)
@RestController
@RequestMapping("/api/v1/feature-flags")
@RequiredArgsConstructor
public class FeatureFlagResource {

    private final FeatureFlagService featureFlagService;

    @Operation(
        summary = "Get all feature flags status",
        description = "Retrieves the status of all feature flags configured in Flagsmith."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getAllFeatureFlags() {
        log.debug("Fetching all feature flags");
        Map<String, Boolean> featureFlags = featureFlagService.getAllFeatureFlags();
        log.info("Retrieved {} feature flags", featureFlags.size());
        return ResponseEntity.ok(ApiResponse.success("Feature flags retrieved successfully", featureFlags));
    }

    @Operation(
        summary = "Check if a specific feature is enabled",
        description = "Checks if a specific feature flag is enabled by its name."
    )
    @GetMapping("/{featureName}")
    public ResponseEntity<ApiResponse<Boolean>> isFeatureEnabled(@PathVariable String featureName) {
        log.debug("Checking feature flag: feature={}", featureName);
        boolean isEnabled = featureFlagService.isFeatureEnabled(featureName);
        log.info("Feature flag status: feature={}, enabled={}", featureName, isEnabled);
        return ResponseEntity.ok(ApiResponse.success("Feature flag status retrieved", isEnabled));
    }
}
