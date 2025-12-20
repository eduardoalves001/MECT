package com.ua.rtmp.service;

import com.ua.rtmp.exception.LlmException;
import com.ua.rtmp.tools.ComponentTools;
import com.ua.rtmp.tools.ThreatModelTools;
import com.ua.rtmp.tools.ThreatTools;
import com.ua.rtmp.tools.VulnerabilityTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final ChatModel chatModel;
    private final ChatMemoryStore chatMemoryStore;
    private final ThreatModelTools threatModelTools;
    private final ComponentTools componentTools;
    private final VulnerabilityTools vulnerabilityTools;
    private final ThreatTools threatTools;

    private String systemPrompt;
    private Assistant assistant;

    interface Assistant {
        String chat(@MemoryId Object memoryId, @UserMessage String userMessage);
    }

    @PostConstruct
    private void initialize() {
        loadSystemPrompt();
        buildAssistant();
    }

    private void loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("agent-prompt.md");
            systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("Loaded system prompt from agent-prompt.md: {} characters", systemPrompt.length());
        } catch (IOException e) {
            log.error("Failed to load system prompt from agent-prompt.md", e);
            throw new RuntimeException("Failed to load system prompt", e);
        }
    }

    private void buildAssistant() {
        log.info("Building AI Assistant with tools and chat memory...");

        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(chatMemoryStore)
                .build();

        assistant = AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .chatMemoryProvider(chatMemoryProvider)
            .tools(threatModelTools, componentTools, vulnerabilityTools, threatTools)
            .systemMessageProvider(chatMemoryId -> systemPrompt)
            .build();

        log.info("AI Assistant initialized successfully");
    }

    public String processMessage(String userMessage, String userId) {
        log.info("Processing chat message: userId={}, messagePreview={}",
                userId, userMessage.substring(0, Math.min(100, userMessage.length())));

        try {
            log.debug("Sending request to Gemini API: userId={}", userId);
            long llmStartTime = System.currentTimeMillis();

            String response = assistant.chat(userId, userMessage);

            long llmDuration = System.currentTimeMillis() - llmStartTime;
            log.info("Gemini response received: userId={}, llmDuration={}ms, responsePreview={}",
                    userId, llmDuration, response.substring(0, Math.min(150, response.length())));

            return response;

        } catch (Exception e) {
            log.error("Error processing chat message: userId={}, errorType={}, message={}",
                    userId, e.getClass().getSimpleName(), e.getMessage(), e);

            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            log.debug("Processing error message for user {}: {}", userId, e.getMessage());

            // Parse rate limit errors (429 TooManyRequests) - check this FIRST
            if (errorMessage.contains("429") || errorMessage.contains("too many requests")
                || errorMessage.contains("rate limit")) {
                Integer retryAfter = parseRetryDelay(errorMessage);
                throw new LlmException(
                    "Rate limit exceeded. The API is receiving too many requests. Please wait a moment and try again.",
                    LlmException.LlmErrorType.RATE_LIMIT,
                    retryAfter != null ? retryAfter : 60
                );
            }

            // Parse quota exceeded errors (actual quota/billing issues)
            if (errorMessage.contains("quota") && errorMessage.contains("exceeded")
                && !errorMessage.contains("request")) {
                Integer retryAfter = parseRetryDelay(errorMessage);
                throw new LlmException(
                    "API quota exceeded. Please try again later or upgrade your plan.",
                    LlmException.LlmErrorType.QUOTA_EXCEEDED,
                    retryAfter
                );
            }

            // Parse authentication errors
            if (errorMessage.contains("unauthorized") || errorMessage.contains("invalid api key")
                || errorMessage.contains("authentication")) {
                throw new LlmException(
                    "AI service authentication failed. Please contact support.",
                    LlmException.LlmErrorType.INVALID_API_KEY
                );
            }

            // Parse timeout errors
            if (errorMessage.contains("timeout") || errorMessage.contains("timed out")) {
                throw new LlmException(
                    "AI service request timed out. Please try again.",
                    LlmException.LlmErrorType.TIMEOUT
                );
            }

            // Parse service unavailable errors
            if (errorMessage.contains("unavailable") || errorMessage.contains("service")
                || e.getClass().getSimpleName().contains("Connect")) {
                throw new LlmException(
                    "AI service is temporarily unavailable. Please try again later.",
                    LlmException.LlmErrorType.SERVICE_UNAVAILABLE
                );
            }

            // Generic processing error
            throw new LlmException(
                "Failed to process your message. Please try again.",
                e,
                LlmException.LlmErrorType.PROCESSING_ERROR
            );
        }
    }

    private Integer parseRetryDelay(String errorMessage) {
        try {
            // Look for patterns like "retry in 51.072456952s" or "retry after 51s"
            String[] words = errorMessage.split("\\s+");
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equals("in") || words[i].equals("after")) {
                    String next = words[i + 1].replaceAll("[^0-9.]", "");
                    if (!next.isEmpty()) {
                        return (int) Math.ceil(Double.parseDouble(next));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse retry delay from error message", e);
        }
        return null;
    }
}
