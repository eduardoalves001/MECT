package com.ua.rtmp.dto.response;

import com.ua.rtmp.model.enums.StrideCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreatsByCategoryDTO {
    private StrideCategory category;
    private Long count;
}