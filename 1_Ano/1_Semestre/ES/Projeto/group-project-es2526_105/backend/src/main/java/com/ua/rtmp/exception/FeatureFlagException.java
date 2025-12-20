package com.ua.rtmp.exception;

public class FeatureFlagException extends RuntimeException {
    
    private final String featureFlagName;
    
    public FeatureFlagException(String featureFlagName) {
        super(String.format("Feature '%s' is currently disabled", featureFlagName));
        this.featureFlagName = featureFlagName;
    }
    
    public FeatureFlagException(String featureFlagName, String message) {
        super(message);
        this.featureFlagName = featureFlagName;
    }
    
    public String getFeatureFlagName() {
        return featureFlagName;
    }
}
