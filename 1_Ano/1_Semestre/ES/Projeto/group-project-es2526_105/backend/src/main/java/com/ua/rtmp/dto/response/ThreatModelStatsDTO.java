package com.ua.rtmp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreatModelStatsDTO {
    private Integer totalComponents;
    private Integer activeThreats;
    private Integer highRiskThreats;
    private Integer mitigatedThreats;
}