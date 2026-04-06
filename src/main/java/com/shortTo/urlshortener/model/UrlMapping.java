package com.shortTo.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data                           // ✅ generates getters, setters, equals, hashCode, toString
@Builder                        // ✅ enables builder pattern
@NoArgsConstructor              // ✅ JPA needs empty constructor to create objects
@AllArgsConstructor             // ✅ Builder needs all-args constructor internally
@Entity
@Table(name = "url_mappings", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)  // ✅ performance
})
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(length = 50)
    private String customAlias;

    @Column(nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
//
//    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiresAt;

}