package com.shortTo.urlshortener.service;

import com.shortTo.urlshortener.dto.AnalyticsResponseDto;
import com.shortTo.urlshortener.dto.ClickTrendDto;
import com.shortTo.urlshortener.model.ClickLog;
import com.shortTo.urlshortener.model.UrlMapping;
import com.shortTo.urlshortener.model.User;
import com.shortTo.urlshortener.repository.ClickLogRepository;
import com.shortTo.urlshortener.repository.UrlRepository;
import com.shortTo.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickLogRepository clickLogRepository;
    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void logClick(String shortCode, String ipAddress, String userAgent) {
        String country = "Unknown";
        String city = "Unknown";

        try {
            // In real world, we'd use a robust IP geo library or service.
            // For this premium feature, we'll use a public API with a fallback for local testing.
            if (ipAddress != null && !ipAddress.equals("127.0.0.1") && !ipAddress.equals("0:0:0:0:0:0:0:1")) {
                String geoUrl = "http://ip-api.com/json/" + ipAddress;
                Map<String, Object> response = restTemplate.getForObject(geoUrl, Map.class);
                if (response != null && "success".equals(response.get("status"))) {
                    country = (String) response.get("country");
                    city = (String) response.get("city");
                }
            } else {
                // MOCK DATA for local development to show off the premium UI
                String[] mockCountries = {"India", "USA", "UK", "Germany", "France", "Japan", "Canada"};
                country = mockCountries[(int) (Math.random() * mockCountries.length)];
                city = "Premium City";
            }
        } catch (Exception e) {
            log.error("Failed to resolve geo data for IP: {}", ipAddress, e);
        }

        ClickLog clickLog = ClickLog.builder()
                .shortCode(shortCode)
                .ipAddress(ipAddress)
                .country(country)
                .city(city)
                .browser(parseBrowser(userAgent))
                .device(parseDevice(userAgent))
                .build();

        clickLogRepository.save(clickLog);
    }

    public AnalyticsResponseDto getUrlAnalytics(String shortCode) {
        // Security Check
        UrlMapping urlMapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        User currentUser = getCurrentUser();
        if (!urlMapping.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to view these analytics");
        }

        List<ClickLog> logs = clickLogRepository.findByShortCode(shortCode);

        // Group by Date for trend
        Map<String, Long> trendMap = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getClickTime().toLocalDate().toString(),
                        Collectors.counting()
                ));

        List<ClickTrendDto> trend = trendMap.entrySet().stream()
                .map(e -> new ClickTrendDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ClickTrendDto::getDate))
                .toList();

        // Stats
        Map<String, Long> countries = logs.stream()
                .collect(Collectors.groupingBy(ClickLog::getCountry, Collectors.counting()));

        Map<String, Long> browsers = logs.stream()
                .collect(Collectors.groupingBy(ClickLog::getBrowser, Collectors.counting()));

        Map<String, Long> devices = logs.stream()
                .collect(Collectors.groupingBy(ClickLog::getDevice, Collectors.counting()));

        return AnalyticsResponseDto.builder()
                .clickTrend(trend)
                .countryStats(countries)
                .browserStats(browsers)
                .deviceStats(devices)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Firefox")) return "Firefox";
        return "Other";
    }

    private String parseDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Mobi")) return "Mobile";
        return "Desktop";
    }
}
