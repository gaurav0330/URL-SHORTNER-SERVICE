package com.shortTo.urlshortener.controller;

import com.shortTo.urlshortener.dto.AuthResponseDto;
import com.shortTo.urlshortener.dto.LoginRequestDto;
import com.shortTo.urlshortener.dto.RegisterRequestDto;
import com.shortTo.urlshortener.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Register and Login endpoints")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @RequestBody @Valid RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @Operation(summary = "Login and get JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @RequestBody @Valid LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }
}