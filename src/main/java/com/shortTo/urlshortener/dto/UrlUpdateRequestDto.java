package com.shortTo.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UrlUpdateRequestDto {

    @Schema(description = "The new original long URL to redirect to",
            example = "https://www.google.com/updated")
    @NotBlank(message = "URL cannot be empty")
    @Pattern(
            regexp = "^(https?://).*",
            message = "URL must start with http:// or https://"
    )
    private String originalUrl;

}
