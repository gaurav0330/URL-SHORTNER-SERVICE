package com.shortTo.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponeDto {
    private String shortCode;
    private String shortUrl;         // full URL e.g. http://localhost:8080/aB3xYz
    private String originalUrl;
    private String customAlias;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

}
