package com.shortTo.urlshortener.exception;

import com.shortTo.urlshortener.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Collect all field error messages into one string
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        ex.printStackTrace();

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.", request.getRequestURI());
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponseDto> handleRateLimit(
            RateLimitException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UrlExpiredException.class)
    public  ResponseEntity<ErrorResponseDto> handleExpired(UrlExpiredException ex,HttpServletRequest request){
        return  buildError(HttpStatus.GONE,ex.getMessage(),request.getRequestURI());
    }




//private helper
    private ResponseEntity<ErrorResponseDto> buildError(
            HttpStatus status, String message, String path) {

        ErrorResponseDto error = ErrorResponseDto.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }
}

