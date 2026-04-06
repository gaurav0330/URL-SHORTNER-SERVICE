package com.shortTo.urlshortener.repository;

import com.shortTo.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping,Long> {

    Optional<UrlMapping> findByShortCodeAndIsActive(String hashCode, boolean isActive);

    boolean existsByShortCode(String shortCode);


    boolean existsByCustomAlias(String customAlias);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + :count WHERE u.shortCode = :shortCode")
    void incrementClickCountBy(@Param("shortCode") String shortCode, @Param("count") long count);
}
