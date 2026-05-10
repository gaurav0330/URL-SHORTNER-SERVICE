package com.shortTo.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "click_logs", indexes = {
        @Index(name = "idx_short_code_logs", columnList = "shortCode")
})
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shortCode;

    private String country;
    private String city;
    private String browser;
    private String device;
    private String ipAddress;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime clickTime = LocalDateTime.now();
}
