package com.ua.rtmp.service;

import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FlagsmithClient flagsmithClient;

    public boolean isFeatureEnabled(String featureName) {
        log.debug("Checking feature flag: featureName={}", featureName);
        try {
            Flags flags = flagsmithClient.getEnvironmentFlags();
            boolean isEnabled = flags.isFeatureEnabled(featureName);
            log.debug("Feature flag status: featureName={}, enabled={}", featureName, isEnabled);
            return isEnabled;
        } catch (FlagsmithClientError e) {
            log.error("Failed to check feature flag: featureName={}, error={}", featureName, e.getMessage());
            return false;
        }
    }

    public boolean isFeatureEnabledForUser(String featureName, String userId) {
        log.debug("Checking feature flag for user: featureName={}, userId={}", featureName, userId);
        try {
            Flags flags = flagsmithClient.getIdentityFlags(userId);
            boolean isEnabled = flags.isFeatureEnabled(featureName);
            log.debug("Feature flag status for user: featureName={}, userId={}, enabled={}", 
                    featureName, userId, isEnabled);
            return isEnabled;
        } catch (FlagsmithClientError e) {
            log.error("Failed to check feature flag for user: featureName={}, userId={}, error={}", 
                    featureName, userId, e.getMessage());
            return false;
        }
    }

    public Map<String, Boolean> getAllFeatureFlags() {
        log.debug("Fetching all feature flags");
        Map<String, Boolean> featureFlags = new HashMap<>();
        try {
            Flags flags = flagsmithClient.getEnvironmentFlags();
            // Get all feature names from the flags object
            flags.getAllFlags().forEach(flag -> {
                try {
                    String featureName = flag.getFeatureName();
                    boolean isEnabled = flags.isFeatureEnabled(featureName);
                    featureFlags.put(featureName, isEnabled);
                    log.debug("Feature flag: name={}, enabled={}", featureName, isEnabled);
                } catch (FlagsmithClientError e) {
                    log.warn("Failed to check individual flag: error={}", e.getMessage());
                }
            });
            log.info("Retrieved {} feature flags", featureFlags.size());
        } catch (FlagsmithClientError e) {
            log.error("Failed to fetch all feature flags: error={}", e.getMessage());
        }
        return featureFlags;
    }
}
