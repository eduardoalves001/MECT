package com.ua.rtmp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.ua.rtmp.config.TestSecurityConfig;
import com.ua.rtmp.config.TestFlagsmithConfig;
import com.ua.rtmp.config.TestLangChainConfig;

@SpringBootTest
@Import({TestSecurityConfig.class, TestFlagsmithConfig.class, TestLangChainConfig.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.enabled=false",
    "spring.main.allow-bean-definition-overriding=true",
    "langchain4j.google-ai-gemini.chat-model.api-key=test-key"
})
class RtmpApplicationTests {

	@Test
	void contextLoads() {
	}

}
