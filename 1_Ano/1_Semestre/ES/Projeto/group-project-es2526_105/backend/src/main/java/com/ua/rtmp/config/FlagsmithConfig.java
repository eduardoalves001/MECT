package com.ua.rtmp.config;

import com.flagsmith.FlagsmithClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlagsmithConfig {

    @Value("${flagsmith.api.url}")
    private String apiUrl;

    @Value("${flagsmith.environment.key}")
    private String environmentKey;

    @Bean
    public FlagsmithClient flagsmithClient() {
        return FlagsmithClient.newBuilder()
                .setApiKey(environmentKey)
                .withApiUrl(apiUrl)
                .build();
    }
}
