package com.shortTo.urlshortener.service;

import com.shortTo.urlshortener.dto.UrlRequestDto;
import com.shortTo.urlshortener.dto.UrlResponeDto;
import com.shortTo.urlshortener.exception.BadRequestException;
import com.shortTo.urlshortener.exception.ResourceNotFoundException;
import com.shortTo.urlshortener.exception.UrlExpiredException;
import com.shortTo.urlshortener.model.UrlMapping;
import com.shortTo.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // enables Mockito
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;        // fake repository

    @Mock
    private RedisTemplate<String, String> redisTemplate;  // fake Redis

    @Mock
    private ValueOperations<String, String> valueOperations;  // fake Redis ops

    @InjectMocks
    private UrlService urlService;              // REAL service, fake dependencies injected

    @BeforeEach
    void setUp() {
        // inject @Value fields manually (Spring not running in unit tests)
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(urlService, "cacheTtlMinutes", 60L);

        // wire fake redis ops
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    //  Shorten URL successfully
    @Test
    void shortenUrl_shouldReturnShortUrl_whenValidRequest() {

        // ARRANGE — set up the scenario
        UrlRequestDto request = new UrlRequestDto();
        request.setOriginalUrl("https://www.google.com");

        UrlMapping savedMapping = UrlMapping.builder()
                .id(1L)
                .originalUrl("https://www.google.com")
                .shortCode("aB3xYz")
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // ACT — call the real method
        UrlResponeDto response = urlService.shortenUrl(request);

        // ASSERT — check the result
        assertThat(response).isNotNull();
        assertThat(response.getOriginalUrl()).isEqualTo("https://www.google.com");
        assertThat(response.getShortUrl()).startsWith("http://localhost:8080/");
        assertThat(response.getClickCount()).isEqualTo(0L);

        // verify save was called exactly once
        verify(urlRepository, times(1)).save(any(UrlMapping.class));
    }

    // Test 2: Custom alias already taken
    @Test
    void shortenUrl_shouldThrowBadRequest_whenAliasAlreadyTaken() {

        // ARRANGE
        UrlRequestDto request = new UrlRequestDto();
        request.setOriginalUrl("https://www.google.com");
        request.setCustomAlias("my-google");

        when(urlRepository.existsByCustomAlias("my-google")).thenReturn(true);

        // ACT + ASSERT — expect exception to be thrown
        assertThatThrownBy(() -> urlService.shortenUrl(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already taken");

        // verify save was NEVER called
        verify(urlRepository, never()).save(any());
    }

    // Test 3: Redirect — cache miss, found in DB
    @Test
    void getOriginalUrl_shouldReturnUrl_whenFoundInDB() {

        // ARRANGE
        UrlMapping mapping = UrlMapping.builder()
                .originalUrl("https://www.google.com")
                .shortCode("aB3xYz")
                .isActive(true)
                .clickCount(5L)
                .createdAt(LocalDateTime.now())
                .build();

        when(valueOperations.get("url:aB3xYz")).thenReturn(null);  // cache miss
        when(urlRepository.findByShortCodeAndIsActive("aB3xYz", true))
                .thenReturn(Optional.of(mapping));

        // ACT
        String result = urlService.getOriginalUrl("aB3xYz");

        // ASSERT
        assertThat(result).isEqualTo("https://www.google.com");
        verify(urlRepository).incrementClickCount("aB3xYz");
    }

    // ─── Test 4: Redirect — cache hit ────────────────────────────────
    @Test
    void getOriginalUrl_shouldReturnUrl_fromCache_withoutHittingDB() {

        // ARRANGE — Redis returns a value (cache hit)
        when(valueOperations.get("url:aB3xYz")).thenReturn("https://www.google.com");

        // ACT
        String result = urlService.getOriginalUrl("aB3xYz");

        // ASSERT
        assertThat(result).isEqualTo("https://www.google.com");

        // DB should NEVER be called on cache hit
        verify(urlRepository, never()).findByShortCodeAndIsActive(any(), any());
    }

    // ─── Test 5: Short code not found ────────────────────────────────
    @Test
    void getOriginalUrl_shouldThrowNotFound_whenShortCodeDoesNotExist() {

        // ARRANGE
        when(valueOperations.get("url:fakecode")).thenReturn(null);
        when(urlRepository.findByShortCodeAndIsActive("fakecode", true))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> urlService.getOriginalUrl("fakecode"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("fakecode");
    }

    // ─── Test 6: Expired URL ─────────────────────────────────────────
    @Test
    void getOriginalUrl_shouldThrowExpired_whenUrlIsExpired() {

        // ARRANGE — URL expired yesterday
        UrlMapping expiredMapping = UrlMapping.builder()
                .originalUrl("https://www.google.com")
                .shortCode("aB3xYz")
                .isActive(true)
                .clickCount(0L)
                .createdAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().minusDays(1))  // expired!
                .build();

        when(valueOperations.get("url:aB3xYz")).thenReturn(null);
        when(urlRepository.findByShortCodeAndIsActive("aB3xYz", true))
                .thenReturn(Optional.of(expiredMapping));

        // ACT + ASSERT
        assertThatThrownBy(() -> urlService.getOriginalUrl("aB3xYz"))
                .isInstanceOf(UrlExpiredException.class);

        // verify it was deactivated in DB
        verify(urlRepository).save(any(UrlMapping.class));
    }
}