package com.ua.rtmp.config;

import com.ua.rtmp.service.FeatureFlagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    private final FeatureFlagService featureFlagService;

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/actuator",
        "/api/v1/feature-flags",
        "/docs"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        
        if (isExcludedPath(requestUri)) {
            return true;
        }

        if (featureFlagService.isFeatureEnabled("maintenance_mode")) {
            log.warn("Request blocked by maintenance mode: method={}, uri={}", request.getMethod(), requestUri);
            sendMaintenanceResponse(response);
            return false;
        }

        return true;
    }

    private boolean isExcludedPath(String requestUri) {
        return EXCLUDED_PATHS.stream()
                .anyMatch(requestUri::startsWith);
    }

    private void sendMaintenanceResponse(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"success\":false," +
            "\"message\":\"System is currently under maintenance. Please try again later.\"," +
            "\"data\":null}"
        );
    }
}
