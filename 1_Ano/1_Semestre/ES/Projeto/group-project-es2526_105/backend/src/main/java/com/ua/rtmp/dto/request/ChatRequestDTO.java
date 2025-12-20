package com.ua.rtmp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat request containing user message and session identifier")
public class ChatRequestDTO {

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 50000, message = "Message cannot exceed 50,000 characters")
    @Schema(description = "User message to send to the AI assistant",
            example = "What are my threat models?",
            required = true)
    private String message;

    @NotBlank(message = "Session ID is required")
    @Size(max = 100, message = "Session ID cannot exceed 100 characters")
    @Schema(description = "Unique session identifier for this chat conversation. Generated client-side to maintain separate conversations per browser tab/device.",
            example = "session-1702472345678-abc123",
            required = true)
    private String sessionId;
}
