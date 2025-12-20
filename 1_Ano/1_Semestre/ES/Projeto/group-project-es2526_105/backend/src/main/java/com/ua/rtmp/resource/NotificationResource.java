package com.ua.rtmp.resource;

import com.ua.rtmp.annotation.RequireFeatureFlag;
import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.dto.response.NotificationResponseDTO;
import com.ua.rtmp.mapper.NotificationMapper;
import com.ua.rtmp.model.Notification;
import com.ua.rtmp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(
    name = "Notification Resource",
    description = "Endpoints for managing user notifications"
)
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@RequireFeatureFlag("comments")
public class NotificationResource {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @Operation(
        summary = "Get all notifications for current user",
        description = "Retrieves all notifications for the authenticated user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getAllNotifications(
            Authentication authentication) {
        log.debug("GET /api/v1/notifications - Fetching all notifications");

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("sub");

        List<Notification> notifications = notificationService.getNotificationsForUser(userId);
        List<NotificationResponseDTO> responses = notificationMapper.toResponseList(notifications);

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", responses));
    }

    @Operation(
        summary = "Get unread notifications for current user",
        description = "Retrieves only unread notifications for the authenticated user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getUnreadNotifications(
            Authentication authentication) {
        log.debug("GET /api/v1/notifications/unread - Fetching unread notifications");

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("sub");

        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(userId);
        List<NotificationResponseDTO> responses = notificationMapper.toResponseList(notifications);

        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully", responses));
    }

    @Operation(
        summary = "Mark a notification as read",
        description = "Marks a specific notification as read"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "UUID of the notification", required = true)
            @PathVariable UUID notificationId,
            Authentication authentication) {
        log.debug("PUT /api/v1/notifications/{}/read - Marking as read", notificationId);

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("sub");

        notificationService.markNotificationAsRead(notificationId, userId);

        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @Operation(
        summary = "Mark all notifications as read",
        description = "Marks all notifications for the authenticated user as read"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            Authentication authentication) {
        log.debug("PUT /api/v1/notifications/read-all - Marking all as read");

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("sub");

        notificationService.markAllNotificationsAsRead(userId);

        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
