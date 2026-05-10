package com.shortTo.urlshortener.controller;

import com.shortTo.urlshortener.exception.PasswordRequiredException;
import com.shortTo.urlshortener.service.AnalyticsService;
import com.shortTo.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Redirect", description = "Handles short URL redirection")
@RestController
@RequiredArgsConstructor
public class RedirectController {
    private final UrlService urlService;
    private final AnalyticsService analyticsService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Operation(
            summary = "Redirect to original URL",
            description = "Redirects to the original URL associated with the short code"
    )
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         @RequestParam(required = false) String p,
                                         HttpServletRequest request) {
        try {
            String originalUrl = urlService.getOriginalUrl(shortCode, p);

            // Capture Analytics
            String ip = request.getRemoteAddr();
            String ua = request.getHeader("User-Agent");
            analyticsService.logClick(shortCode, ip, ua);

            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
        } catch (PasswordRequiredException e) {
            // Redirect to frontend unlock page
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/unlock/" + shortCode))
                    .build();
        }
    }

}
