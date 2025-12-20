package com.ua.rtmp.resource;

import com.ua.rtmp.annotation.RequireFeatureFlag;
import com.ua.rtmp.dto.request.CommentRequestDTO;
import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.dto.response.CommentResponseDTO;
import com.ua.rtmp.mapper.ComponentCommentMapper;
import com.ua.rtmp.mapper.VulnerabilityCommentMapper;
import com.ua.rtmp.model.ComponentComment;
import com.ua.rtmp.model.VulnerabilityComment;
import com.ua.rtmp.service.ComponentCommentService;
import com.ua.rtmp.service.VulnerabilityCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(
    name = "Comment Resource",
    description = "Endpoints for managing comments and discussions on vulnerabilities and components"
)
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@RequireFeatureFlag("comments")
public class CommentResource {

    private final ComponentCommentService componentCommentService;
    private final VulnerabilityCommentService vulnerabilityCommentService;
    private final ComponentCommentMapper componentCommentMapper;
    private final VulnerabilityCommentMapper vulnerabilityCommentMapper;

    @Operation(
        summary = "Create a new comment",
        description = "Creates a new comment or reply on a vulnerability or component"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PreAuthorize("hasRole('comment:create')")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponseDTO>> createComment(
            @Valid @RequestBody CommentRequestDTO request,
            Authentication authentication) {
        log.debug("POST /api/v1/comments - Creating new comment");

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String authorUserId = jwt.getClaimAsString("sub");
        String authorUsername = jwt.getClaimAsString("preferred_username");

        CommentResponseDTO response;

        if (request.getVulnerabilityId() != null) {
            VulnerabilityComment comment = vulnerabilityCommentService.createComment(
                    request.getContent(),
                    request.getVulnerabilityId(),
                    request.getParentCommentId(),
                    authorUserId,
                    authorUsername
            );
            response = vulnerabilityCommentMapper.toResponse(comment);
        } else if (request.getComponentId() != null) {
            ComponentComment comment = componentCommentService.createComment(
                    request.getContent(),
                    request.getComponentId(),
                    request.getParentCommentId(),
                    authorUserId,
                    authorUsername
            );
            response = componentCommentMapper.toResponse(comment);
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("Either vulnerabilityId or componentId must be provided"));
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment created successfully", response));
    }

    @Operation(
        summary = "Get comments for a vulnerability",
        description = "Retrieves all comments and replies for a specific vulnerability"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vulnerability not found")
    })
    @PreAuthorize("hasRole('comment:read')")
    @GetMapping("/vulnerability/{vulnerabilityId}")
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getCommentsByVulnerability(
            @Parameter(description = "UUID of the vulnerability", required = true)
            @PathVariable UUID vulnerabilityId) {
        log.debug("GET /api/v1/comments/vulnerability/{} - Fetching comments", vulnerabilityId);

        List<VulnerabilityComment> comments = vulnerabilityCommentService.getCommentsByVulnerabilityId(vulnerabilityId);
        List<CommentResponseDTO> responses = vulnerabilityCommentMapper.toResponseList(comments);

        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", responses));
    }

    @Operation(
        summary = "Get comments for a component",
        description = "Retrieves all comments and replies for a specific component"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Component not found")
    })
    @PreAuthorize("hasRole('comment:read')")
    @GetMapping("/component/{componentId}")
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getCommentsByComponent(
            @Parameter(description = "UUID of the component", required = true)
            @PathVariable UUID componentId) {
        log.debug("GET /api/v1/comments/component/{} - Fetching comments", componentId);

        List<ComponentComment> comments = componentCommentService.getCommentsByComponentId(componentId);
        List<CommentResponseDTO> responses = componentCommentMapper.toResponseList(comments);

        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", responses));
    }

    @Operation(
        summary = "Delete a comment",
        description = "Deletes a comment (only the author can delete their own comment)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to delete this comment"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @PreAuthorize("hasRole('comment:delete')")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "UUID of the comment", required = true)
            @PathVariable UUID commentId,
            @RequestParam(required = false) String type,
            Authentication authentication) {
        log.debug("DELETE /api/v1/comments/{} - Deleting comment, type={}", commentId, type);

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaimAsString("sub");

        try {
            if ("vulnerability".equals(type)) {
                vulnerabilityCommentService.deleteComment(commentId, userId);
            } else if ("component".equals(type)) {
                componentCommentService.deleteComment(commentId, userId);
            } else {
                try {
                    vulnerabilityCommentService.deleteComment(commentId, userId);
                } catch (Exception e) {
                    componentCommentService.deleteComment(commentId, userId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete comment: {}", e.getMessage());
            throw e;
        }

        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }
}
