package com.shortTo.urlshortener.controller;

import com.shortTo.urlshortener.dto.AnalyticsResponseDto;
import com.shortTo.urlshortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for URL analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get analytics for a short URL", description = "Returns click trends, country, browser and device statistics")
    @GetMapping("/{shortCode}")
    public ResponseEntity<AnalyticsResponseDto> getAnalytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getUrlAnalytics(shortCode));
    }
}
