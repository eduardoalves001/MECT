package com.ua.rtmp.model.enums;

public enum RiskLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    MINIMAL;

    public static RiskLevel fromRiskScore(Integer riskScore) {
        if (riskScore == null) return MINIMAL;
        if (riskScore >= 20) return CRITICAL;
        if (riskScore >= 15) return HIGH;
        if (riskScore >= 10) return MEDIUM;
        if (riskScore >= 5) return LOW;
        return MINIMAL;
    }
}
