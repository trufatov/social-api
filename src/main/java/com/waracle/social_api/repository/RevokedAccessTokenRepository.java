package com.waracle.social_api.repository;

import com.waracle.social_api.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

    int deleteByExpiresAtBefore(LocalDateTime cutoff);
}
