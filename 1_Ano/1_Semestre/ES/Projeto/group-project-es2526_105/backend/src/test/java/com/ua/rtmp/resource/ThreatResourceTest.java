package com.ua.rtmp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.rtmp.model.Threat;
import com.ua.rtmp.model.enums.StrideCategory;
import com.ua.rtmp.service.ThreatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ua.rtmp.util.ThreatTestDataBuilder.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import com.ua.rtmp.config.TestFlagsmithConfig;
import com.ua.rtmp.service.FeatureFlagService;

@WebMvcTest(value = ThreatResource.class)
@Import({TestFlagsmithConfig.class, com.ua.rtmp.config.TestSecurityConfig.class, FeatureFlagService.class, com.ua.rtmp.config.MaintenanceModeInterceptor.class, com.ua.rtmp.config.WebConfig.class})
class ThreatResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThreatService threatService;

    private Threat sqlInjectionThreat;
    private Threat xssThreat;
    private UUID threatId;

    @BeforeEach
    void setUp() {
        threatId = UUID.randomUUID();

        sqlInjectionThreat = aSqlInjectionThreat()
                .withId(threatId)
                .build();

        xssThreat = anXssThreat()
                .withRandomId()
                .build();
    }

    @Test
    void getAllThreats_WhenThreatsExist_ShouldReturnThreatsWithSuccessResponse() throws Exception {
        // Given
        List<Threat> threats = Arrays.asList(sqlInjectionThreat, xssThreat);
        when(threatService.getAllThreats()).thenReturn(threats);

        // When & Then
        mockMvc.perform(get("/api/v1/threats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Threats retrieved successfully")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id", is(threatId.toString())))
                .andExpect(jsonPath("$.data[0].name", is("SQL Injection")))
                .andExpect(jsonPath("$.data[0].description", is("An attacker can inject malicious SQL statements")))
                .andExpect(jsonPath("$.data[0].category", is("TAMPERING")))
                .andExpect(jsonPath("$.data[1].name", is("Cross-Site Scripting")))
                .andExpect(jsonPath("$.data[1].description", is("Malicious scripts executed in user browser")))
                .andExpect(jsonPath("$.data[1].category", is("SPOOFING")));

        verify(threatService, times(1)).getAllThreats();
    }

    @Test
    void getAllThreats_WhenNoThreatsExist_ShouldReturnEmptyListWithSuccessResponse() throws Exception {
        // Given
        when(threatService.getAllThreats()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/threats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Threats retrieved successfully")))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(threatService, times(1)).getAllThreats();
    }

    @Test
    void getThreatById_WhenThreatExists_ShouldReturnThreatWithSuccessResponse() throws Exception {
        // Given
        when(threatService.getThreatById(threatId)).thenReturn(Optional.of(sqlInjectionThreat));

        // When & Then
        mockMvc.perform(get("/api/v1/threats/{id}", threatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Threat retrieved successfully")))
                .andExpect(jsonPath("$.data.id", is(threatId.toString())))
                .andExpect(jsonPath("$.data.name", is("SQL Injection")))
                .andExpect(jsonPath("$.data.description", is("An attacker can inject malicious SQL statements")))
                .andExpect(jsonPath("$.data.category", is("TAMPERING")));

        verify(threatService, times(1)).getThreatById(threatId);
    }

    @Test
    void getThreatById_WhenThreatDoesNotExist_ShouldReturnNotFoundResponse() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(threatService.getThreatById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/threats/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Threat not found")))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(threatService, times(1)).getThreatById(nonExistentId);
    }

    @Test
    void getThreatById_WithInvalidUUID_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/threats/{id}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid value")));

        verify(threatService, never()).getThreatById(any());
    }

    @Test
    void getThreatById_WithThreatHavingNullDescription_ShouldReturnSuccessResponse() throws Exception {
        // Given
        Threat threatWithNullDescription = aSqlInjectionThreat()
                .withId(threatId)
                .withNullDescription()
                .build();
        when(threatService.getThreatById(threatId)).thenReturn(Optional.of(threatWithNullDescription));

        // When & Then
        mockMvc.perform(get("/api/v1/threats/{id}", threatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(threatId.toString())))
                .andExpect(jsonPath("$.data.name", is("SQL Injection")))
                .andExpect(jsonPath("$.data.description").value(nullValue()))
                .andExpect(jsonPath("$.data.category", is("TAMPERING")));

        verify(threatService, times(1)).getThreatById(threatId);
    }

    @Test
    void getThreatById_WithThreatHavingNullCategory_ShouldReturnSuccessResponse() throws Exception {
        // Given
        Threat threatWithNullCategory = aSqlInjectionThreat()
                .withId(threatId)
                .withNullCategory()
                .build();
        when(threatService.getThreatById(threatId)).thenReturn(Optional.of(threatWithNullCategory));

        // When & Then
        mockMvc.perform(get("/api/v1/threats/{id}", threatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(threatId.toString())))
                .andExpect(jsonPath("$.data.name", is("SQL Injection")))
                .andExpect(jsonPath("$.data.description", is("An attacker can inject malicious SQL statements")))
                .andExpect(jsonPath("$.data.category").value(nullValue()));

        verify(threatService, times(1)).getThreatById(threatId);
    }

    @Test
    void getAllThreats_WithAllStrideCategories_ShouldReturnAllCategories() throws Exception {
        // Given
        List<Threat> threats = Arrays.asList(
                createThreatWithCategory("Threat 1", StrideCategory.SPOOFING),
                createThreatWithCategory("Threat 2", StrideCategory.TAMPERING),
                createThreatWithCategory("Threat 3", StrideCategory.REPUDIATION),
                createThreatWithCategory("Threat 4", StrideCategory.INFORMATION_DISCLOSURE),
                createThreatWithCategory("Threat 5", StrideCategory.DENIAL_OF_SERVICE),
                createThreatWithCategory("Threat 6", StrideCategory.ELEVATION_OF_PRIVILEGE)
        );
        when(threatService.getAllThreats()).thenReturn(threats);

        // When & Then
        mockMvc.perform(get("/api/v1/threats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].category", is("SPOOFING")))
                .andExpect(jsonPath("$.data[1].category", is("TAMPERING")))
                .andExpect(jsonPath("$.data[2].category", is("REPUDIATION")))
                .andExpect(jsonPath("$.data[3].category", is("INFORMATION_DISCLOSURE")))
                .andExpect(jsonPath("$.data[4].category", is("DENIAL_OF_SERVICE")))
                .andExpect(jsonPath("$.data[5].category", is("ELEVATION_OF_PRIVILEGE")));

        verify(threatService, times(1)).getAllThreats();
    }

    @Test
    void endpoints_ShouldHaveCorrectPaths() throws Exception {
        // Test base path mapping
        when(threatService.getAllThreats()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/threats"))
                .andExpect(status().isOk());

        // Test parameterized path mapping
        when(threatService.getThreatById(any())).thenReturn(Optional.of(sqlInjectionThreat));

        mockMvc.perform(get("/api/v1/threats/{id}", threatId))
                .andExpect(status().isOk());
    }

    private Threat createThreatWithCategory(String name, StrideCategory category) {
        Threat threat = new Threat();
        threat.setId(UUID.randomUUID());
        threat.setName(name);
        threat.setDescription("Test description");
        threat.setCategory(category);
        return threat;
    }
}