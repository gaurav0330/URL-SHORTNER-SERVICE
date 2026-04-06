package com.shortTo.urlshortener.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.rate-limit.max-requests}")
    private int maxRequests;

    @Value("${app.rate-limit.window-minutes}")
    private int windowMinutes;

    private static final String RATE_PREFIX = "rate:";


    public boolean isAllowed(String ipAddress){
        String key = RATE_PREFIX + ipAddress;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            // First request from this IP → set TTL (expiry window starts now)
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }

        return count <= maxRequests;
    }
}
