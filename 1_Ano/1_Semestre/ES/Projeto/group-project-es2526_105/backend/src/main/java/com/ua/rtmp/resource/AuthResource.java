package com.ua.rtmp.resource;

import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.dto.response.UserInfoResponse;
import com.ua.rtmp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(
    name = "Authentication Resource",
    description = "Endpoints for authentication and user information. " +
                  "Provides operations to retrieve authenticated user details and test authentication status."
)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    @Operation(
        summary = "Get authenticated user information",
        description = "Retrieves the currently authenticated user's information from the JWT token. " +
                      "Returns user details including subject, email, name, preferred username, and roles.",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(Authentication authentication) {
        log.debug("Fetching user information");
        try {
            UserInfoResponse userInfo = authService.getUserInfo(authentication);
            log.info("Retrieved user information: username={}", userInfo.getPreferredUsername());
            return ResponseEntity.ok(ApiResponse.success("User information retrieved successfully", userInfo));
        } catch (IllegalArgumentException e) {
            log.error("Failed to retrieve user information: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "Test authentication status",
        description = "Simple endpoint to test if the authentication is working correctly. " +
                      "Returns a success message if the user is authenticated.",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testAuth(Authentication authentication) {
        log.debug("Testing authentication");
        try {
            String username = authService.getAuthenticatedUsername(authentication);
            log.info("Authentication test successful: username={}", username);
            return ResponseEntity.ok(
                ApiResponse.success("Authentication successful! Logged in as: " + username, username)
            );
        } catch (IllegalArgumentException e) {
            log.error("Authentication test failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
