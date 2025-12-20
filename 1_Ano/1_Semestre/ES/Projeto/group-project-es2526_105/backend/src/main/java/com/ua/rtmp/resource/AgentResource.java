package com.ua.rtmp.resource;

import com.ua.rtmp.dto.request.ChatRequestDTO;
import com.ua.rtmp.dto.response.ApiResponse;
import com.ua.rtmp.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(
    name = "Agent Resource",
    description = "Endpoints for interacting with the RTMP AI Assistant. " +
                  "Provides intelligent assistance for threat modeling, security questions, " +
                  "and platform navigation guidance."
)
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentResource {

    private final AgentService agentService;

    @Operation(
        summary = "Chat with the AI assistant",
        description = "Send a message to the RTMP AI Assistant and receive intelligent responses. " +
                      "The assistant can help with:\n" +
                      "- Understanding STRIDE threat modeling methodology\n" +
                      "- Navigating RTMP platform features\n" +
                      "- Security best practices and vulnerability guidance\n" +
                      "- Analyzing your threat models, components, and vulnerabilities\n" +
                      "- Providing personalized risk insights based on your data\n\n" +
                      "**Session Management**: Each browser tab/device maintains a separate conversation using a unique sessionId. " +
                      "The sessionId must be generated client-side and included with every request to maintain conversation context.\n\n" +
                      "**Tool Access**: The assistant has secure, read-only access to your threat modeling data via specialized tools."
    )
    @PreAuthorize("hasRole('chatbot:use')")
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(
            @Valid @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        String userEmail = jwt.getClaimAsString("email");
        String sessionId = request.getSessionId();
        String memoryId = userId + ":" + sessionId;

        log.info("Received chat request: userId={}, sessionId={}, email={}, messageLength={}",
                userId, sessionId, userEmail, request.getMessage().length());
        log.debug("Chat request message: memoryId={}, message={}", memoryId, request.getMessage());

        long startTime = System.currentTimeMillis();

        String response = agentService.processMessage(
            request.getMessage(),
            memoryId
        );

        long duration = System.currentTimeMillis() - startTime;
        log.info("Chat response generated: userId={}, duration={}ms, responseLength={}",
                userId, duration, response.length());
        log.debug("Chat response content: userId={}, response={}", userId, response);

        return ResponseEntity.ok(
            ApiResponse.success("Assistant response generated successfully", response)
        );
    }
}
