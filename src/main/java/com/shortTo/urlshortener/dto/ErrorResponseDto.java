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
public class ErrorResponseDto {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}