package com.shortTo.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UrlRequestDto {

    @Schema(description = "The original long URL to shorten",
            example = "https://www.google.com")
    @NotBlank(message = "URL cannot be empty")
    @Pattern(
            regexp = "^(https?://).*",
            message = "URL must start with http:// or https://"
    )
    private String originalUrl;

    @Schema(description = "Optional custom alias for the short URL",
            example = "my-google")
    @Size(min = 3, max = 30, message = "Alias must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]*$",
            message = "Alias can only contain letters, numbers, hyphens and underscores"
    )
    private String customAlias;   // optional — user may or may not send this

    @Schema(description = "Optional expiry date for the short URL",
            example = "2027-01-01T00:00:00")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiresAt;

}