package com.shortTo.urlshortener.controller;

import com.shortTo.urlshortener.dto.UrlRequestDto;
import com.shortTo.urlshortener.dto.UrlResponeDto;
import com.shortTo.urlshortener.dto.UrlUpdateRequestDto;
import com.shortTo.urlshortener.exception.RateLimitException;
import com.shortTo.urlshortener.service.RateLimiterService;
import com.shortTo.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;




@Tag(name = "URL Shortener", description = "Endpoints for shortening and managing URLs")
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;


    @Operation(
            summary = "Shorten a URL",
            description = "Takes a long URL and returns a shortened version. Supports custom aliases and expiry."
    )
    @PostMapping
    public ResponseEntity<UrlResponeDto> shortenUrl(
            @RequestBody @Valid UrlRequestDto request , HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();

        if (!rateLimiterService.isAllowed(ip)) {
            throw new RateLimitException("Too many requests. Max " + 10 + " per minute allowed.");
        }

        UrlResponeDto response = urlService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get URL stats",
            description = "Returns click count and metadata for a short URL"
    )
    @GetMapping("/{shortCode}/stats")
    public  ResponseEntity<UrlResponeDto> getStats(@PathVariable String shortCode){
        UrlResponeDto stats = urlService.getStats(shortCode);
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<?> getAllUrls() {

        return ResponseEntity.ok(
                urlService.getAllUrls()
        );
    }

    @Operation(
            summary = "Update a URL",
            description = "Updates the original destination URL of an existing short URL"
    )
    @PutMapping("/{id}")
    public ResponseEntity<UrlResponeDto> updateUrl(@PathVariable Long id, @RequestBody @Valid UrlUpdateRequestDto request) {
        UrlResponeDto response = urlService.updateUrl(id, request.getOriginalUrl());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete a URL",
            description = "Soft-deletes a URL by setting it as inactive"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id);
        return ResponseEntity.noContent().build();
    }
}