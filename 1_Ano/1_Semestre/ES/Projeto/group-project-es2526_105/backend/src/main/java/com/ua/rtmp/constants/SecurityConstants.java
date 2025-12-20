package com.ua.rtmp.constants;

public class SecurityConstants {

    public static final String[] PUBLIC_PATHS = {
        "/actuator/health",
        "/api/v1/feature-flags",
        "/api/v1/feature-flags/**",
        "/docs",
        "/docs/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**"
    };
}
