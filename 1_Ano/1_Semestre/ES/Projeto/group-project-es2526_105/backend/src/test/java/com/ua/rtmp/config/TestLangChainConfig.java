package com.ua.rtmp.config;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestLangChainConfig {

    @Bean
    @Primary
    public ChatModel chatModel() {
        // Return a mock ChatModel for testing
        // The AgentService won't actually be called in RtmpApplicationTests
        return mock(ChatModel.class);
    }
}
