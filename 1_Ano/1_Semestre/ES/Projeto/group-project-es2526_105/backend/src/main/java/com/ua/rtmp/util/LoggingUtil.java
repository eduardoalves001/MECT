package com.ua.rtmp.util;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class LoggingUtil {

    private LoggingUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void setRequestContext(String requestId, Authentication authentication) {
        MDC.put("requestId", requestId);
        
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaimAsString("sub");
                String username = jwt.getClaimAsString("preferred_username");
                
                if (userId != null) {
                    MDC.put("userId", userId);
                }
                if (username != null) {
                    MDC.put("username", username);
                }
            } catch (Exception e) {
                // Silently handle case where authentication is not JWT-based
            }
        }
    }

    public static void setOperationContext(String operationType, String resourceType, String resourceId) {
        if (operationType != null) {
            MDC.put("operationType", operationType);
        }
        if (resourceType != null) {
            MDC.put("resourceType", resourceType);
        }
        if (resourceId != null) {
            MDC.put("resourceId", resourceId);
        }
    }

    public static void clearRequestContext() {
        MDC.remove("requestId");
        MDC.remove("userId");
        MDC.remove("username");
    }

    public static void clearOperationContext() {
        MDC.remove("operationType");
        MDC.remove("resourceType");
        MDC.remove("resourceId");
    }

    public static void clearAllContext() {
        MDC.clear();
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
