package com.shortTo.urlshortener.repository;

import com.shortTo.urlshortener.model.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {
    List<ClickLog> findByShortCode(String shortCode);
}
