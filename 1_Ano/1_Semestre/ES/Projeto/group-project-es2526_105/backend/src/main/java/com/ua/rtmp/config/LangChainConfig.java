package com.ua.rtmp.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChainConfig {

    @Value("${langchain4j.google-ai-gemini.chat-model.api-key:}")
    private String apiKey;

    @Value("${langchain4j.google-ai-gemini.chat-model.model-name:gemini-2.5-flash}")
    private String modelName;

    @Value("${langchain4j.google-ai-gemini.chat-model.temperature:0.7}")
    private Double temperature;

    @Value("${langchain4j.google-ai-gemini.chat-model.log-requests:false}")
    private Boolean logRequests;

    @Value("${langchain4j.google-ai-gemini.chat-model.log-responses:false}")
    private Boolean logResponses;

    @Bean
    public ChatModel chatModel() {
        // If no API key is configured, throw an exception to prevent application startup
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "LangChain4j Google AI Gemini API key is not configured. " +
                    "Please set 'langchain4j.google-ai.api-key' in your configuration."
            );
        }

        log.info("Initializing Gemini ChatModel: model={}, apiKeyPrefix={}...",
                modelName, apiKey.substring(0, Math.min(10, apiKey.length())));

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(0)  // Disable retries to prevent duplicate requests on rate limit errors
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
