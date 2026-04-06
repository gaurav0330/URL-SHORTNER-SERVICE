package com.shortTo.urlshortener.service;

import com.shortTo.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickFlushService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UrlRepository urlRepository;

    private static final String CLICK_PREFIX = "clicks:";

    // Runs every 5 minutes automatically
    @Scheduled(fixedRateString = "${app.click-flush.interval-ms:300000}")
    @Transactional
    public void flushClicksToDB() {

        // 1. Find all click keys in Redis e.g. "clicks:aB3xYz"
        Set<String> keys = redisTemplate.keys(CLICK_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        log.info("Flushing clicks to DB for {} URLs", keys.size());

        for (String key : keys) {
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) continue;

            long clickCount = Long.parseLong(value);
            String shortCode = key.replace(CLICK_PREFIX, "");

            // 2. Add accumulated count to MySQL in one shot
            urlRepository.incrementClickCountBy(shortCode, clickCount);

            // 3. Delete from Redis — count flushed ✅
            redisTemplate.delete(key);

            log.info("Flushed {} clicks for shortCode: {}", clickCount, shortCode);
        }
    }
}