package com.ua.rtmp.config;

import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

@TestConfiguration
public class TestFlagsmithConfig {

    @Bean
    @Primary
    public FlagsmithClient flagsmithClient() {
        FlagsmithClient mockClient = mock(FlagsmithClient.class);
        Flags mockFlags = mock(Flags.class);

        try {
            when(mockClient.getEnvironmentFlags()).thenReturn(mockFlags);
            when(mockClient.getIdentityFlags(anyString())).thenReturn(mockFlags);

            when(mockFlags.isFeatureEnabled(anyString())).thenReturn(true);
            
            when(mockFlags.isFeatureEnabled("maintenance_mode")).thenReturn(false);
            
            when(mockFlags.getAllFlags()).thenReturn(new ArrayList<>());

        } catch (FlagsmithClientError e) {
            throw new RuntimeException(e);
        }

        return mockClient;
    }
}
