package com.shortTo.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDto {
    private List<ClickTrendDto> clickTrend;
    private Map<String, Long> countryStats;
    private Map<String, Long> browserStats;
    private Map<String, Long> deviceStats;
}
