package com.shortTo.urlshortener.config;

import com.shortTo.urlshortener.service.JwtService;
import com.shortTo.urlshortener.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no token needed
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/{shortCode}").permitAll()
                        // Everything else needs JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain)
                    throws IOException, jakarta.servlet.ServletException {

                String authHeader = request.getHeader("Authorization");

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    chain.doFilter(request, response);
                    return;
                }

                String token = authHeader.substring(7);

                if (jwtService.isTokenValid(token)) {
                    String email = jwtService.extractEmail(token);
                    var user = userRepository.findByEmail(email);

                    if (user.isPresent()) {
                        var authToken = new org.springframework.security.authentication
                                .UsernamePasswordAuthenticationToken(
                                email, null, java.util.List.of());
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().setAuthentication(authToken);
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }
}