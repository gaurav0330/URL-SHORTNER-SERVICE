package com.shortTo.urlshortener.service;

import com.shortTo.urlshortener.dto.UrlRequestDto;
import com.shortTo.urlshortener.dto.UrlResponeDto;
import com.shortTo.urlshortener.exception.BadRequestException;
import com.shortTo.urlshortener.exception.ResourceNotFoundException;
import com.shortTo.urlshortener.exception.UrlExpiredException;
import com.shortTo.urlshortener.model.UrlMapping;
import com.shortTo.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;   // ✅ correct import
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.cache.ttl-minutes}")
    private long cacheTtlMinutes;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String CACHE_PREFIX = "url:";

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    // Shorten URL

    @Transactional
    public UrlResponeDto shortenUrl(UrlRequestDto request) {

        String code = resolveShortCode(request);

        UrlMapping mapping = UrlMapping.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(code)
                .customAlias(request.getCustomAlias())
                .expiresAt(request.getExpiresAt())
                .build();

        UrlMapping saved = urlRepository.save(mapping);

        return toResponseDto(saved);
    }

    // ─── Redirect ─────────────────────────────────────────────────────
    @Transactional
    public String getOriginalUrl(String shortCode) {

        String cacheKey = CACHE_PREFIX + shortCode;
        String cachedUrl  = redisTemplate.opsForValue().get(cacheKey);

        if(cachedUrl  != null){
            incrementClickInRedis(shortCode);
            return cachedUrl;
        }
        UrlMapping mapping = urlRepository
                .findByShortCodeAndIsActive(shortCode, true)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found or inactive" + shortCode));

        checkExpiry(mapping);

        redisTemplate.opsForValue().set(
                cacheKey,
                mapping.getOriginalUrl(),
                cacheTtlMinutes,
                TimeUnit.MINUTES
        );

        incrementClickInRedis(shortCode);

        return mapping.getOriginalUrl();
    }

    private void evictCache(String shortCode) {
        redisTemplate.delete(CACHE_PREFIX + shortCode);
    }

    public UrlResponeDto getStats(String shortCode){
        UrlMapping mapping = urlRepository.findByShortCodeAndIsActive(shortCode,true).orElseThrow(() ->new ResourceNotFoundException("URL not found: " + shortCode ));
        return toResponseDto(mapping);
    }


//    ALL ARE PRIVATE AND HELPER METHOD

    // if customALias then generate unquie code for it
    private String resolveShortCode(UrlRequestDto request) {
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (urlRepository.existsByCustomAlias(request.getCustomAlias())) {
                throw new BadRequestException("Custom alias already taken" + request.getCustomAlias());
            }
            return request.getCustomAlias();
        }
        return generateUniqueCode();
    }

//    TO GENERATE UNIQUE CODE
    private String generateUniqueCode() {
        String code;

        do {
            code = generateRandomCode();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

//    Function to generate thr randomCode (IF NOT UNIQUE)
    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);

        for(int i = 0 ; i < SHORT_CODE_LENGTH ; i++){
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }


    private UrlResponeDto toResponseDto(UrlMapping mapping) {
        return UrlResponeDto.builder()
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .customAlias(mapping.getCustomAlias())
                .clickCount(mapping.getClickCount())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .build();
    }

    public void checkExpiry(UrlMapping mapping){
        if(mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {

            mapping.setIsActive(false);
        urlRepository.save(mapping);
        evictCache(mapping.getShortCode());
        throw new UrlExpiredException("This URL has expired: " + mapping.getShortCode());
    }
    }

    private static final String CLICK_PREFIX = "clicks:";

    private void incrementClickInRedis(String shortCode) {
        redisTemplate.opsForValue().increment(CLICK_PREFIX + shortCode);
    }
}