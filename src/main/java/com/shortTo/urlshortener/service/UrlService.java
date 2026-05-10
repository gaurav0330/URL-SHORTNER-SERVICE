package com.shortTo.urlshortener.service;

import com.shortTo.urlshortener.dto.UrlRequestDto;
import com.shortTo.urlshortener.dto.UrlResponeDto;
import com.shortTo.urlshortener.exception.BadRequestException;
import com.shortTo.urlshortener.exception.ResourceNotFoundException;
import com.shortTo.urlshortener.exception.UrlExpiredException;
import com.shortTo.urlshortener.model.UrlMapping;
import com.shortTo.urlshortener.model.User;
import com.shortTo.urlshortener.repository.UrlRepository;
import com.shortTo.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;   // ✅ correct import
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public UrlResponeDto shortenUrl(UrlRequestDto request) {

        String code = resolveShortCode(request);

        UrlMapping mapping = UrlMapping.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(code)
                .customAlias(request.getCustomAlias())
                .expiresAt(request.getExpiresAt())
                .user(getCurrentUser())
                .category(request.getCategory())
                .password(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null)
                .build();

        UrlMapping saved = urlRepository.save(mapping);

        return toResponseDto(saved);
    }

    @Transactional
    public UrlResponeDto updateUrl(Long id, String newOriginalUrl) {
        UrlMapping mapping = urlRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));
        
        mapping.setOriginalUrl(newOriginalUrl);
        UrlMapping updated = urlRepository.save(mapping);
        evictCache(mapping.getShortCode());
        return toResponseDto(updated);
    }

    // ─── Redirect ─────────────────────────────────────────────────────
    @Transactional
    public String getOriginalUrl(String shortCode, String password) {
        UrlMapping mapping = urlRepository
                .findByShortCodeAndIsActive(shortCode, true)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found or inactive: " + shortCode));

        checkExpiry(mapping);

        // Check Password
        if (mapping.getPassword() != null) {
            if (password == null || !passwordEncoder.matches(password, mapping.getPassword())) {
                throw new com.shortTo.urlshortener.exception.PasswordRequiredException("Password required or incorrect");
            }
        } else {
            // Only cache non-protected URLs for now
            String cacheKey = CACHE_PREFIX + shortCode;
            redisTemplate.opsForValue().set(
                    cacheKey,
                    mapping.getOriginalUrl(),
                    cacheTtlMinutes,
                    TimeUnit.MINUTES
            );
        }

        incrementClickInRedis(shortCode);
        return mapping.getOriginalUrl();
    }

    public String getOriginalUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            incrementClickInRedis(shortCode);
            return cachedUrl;
        }

        return getOriginalUrl(shortCode, null);
    }

    private void evictCache(String shortCode) {
        redisTemplate.delete(CACHE_PREFIX + shortCode);
    }

    public UrlResponeDto getStats(String shortCode){
        UrlMapping mapping = urlRepository.findByShortCodeAndIsActive(shortCode,true).orElseThrow(() ->new ResourceNotFoundException("URL not found: " + shortCode ));
        return toResponseDto(mapping);
    }

    public List<UrlResponeDto> getAllUrls() {

        return urlRepository
                .findByUser(getCurrentUser())
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

//    ALL ARE PRIVATE AND HELPER METHOD

    // if customALias then generate unique code for it
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
        String pendingClicksStr = redisTemplate.opsForValue().get(CLICK_PREFIX + mapping.getShortCode());
        long pendingClicks = pendingClicksStr != null ? Long.parseLong(pendingClicksStr) : 0L;
        long totalClicks = mapping.getClickCount() + pendingClicks;

        return UrlResponeDto.builder()
                .id(mapping.getId())
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .customAlias(mapping.getCustomAlias())
                .clickCount(totalClicks)
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .isActive(mapping.getIsActive())
                .category(mapping.getCategory())
                .hasPassword(mapping.getPassword() != null)
                .build();
    }

    @Transactional
    public void deleteUrl(Long id) {
        UrlMapping mapping = urlRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));
        mapping.setIsActive(false);
        urlRepository.save(mapping);
        evictCache(mapping.getShortCode());
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