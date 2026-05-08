package com.shortTo.urlshortener.service;

import com.shortTo.urlshortener.dto.AuthResponseDto;
import com.shortTo.urlshortener.dto.LoginRequestDto;
import com.shortTo.urlshortener.dto.RegisterRequestDto;
import com.shortTo.urlshortener.exception.BadRequestException;
import com.shortTo.urlshortener.exception.ResourceNotFoundException;
import com.shortTo.urlshortener.model.User;
import com.shortTo.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ─── Register ─────────────────────────────────────────────────────
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        // Build user with hashed password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash ✅
                .build();

        userRepository.save(user);

        // Generate JWT
        String token = jwtService.generateToken(user.getEmail());

        return AuthResponseDto.builder()
                .accessToken(token)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    // ─── Login ────────────────────────────────────────────────────────
    public AuthResponseDto login(LoginRequestDto request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Compare password with stored hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        // Generate JWT
        String token = jwtService.generateToken(user.getEmail());

        return AuthResponseDto.builder()
                .accessToken(token)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}