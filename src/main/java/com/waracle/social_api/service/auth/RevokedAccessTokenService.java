package com.waracle.social_api.service.auth;

import com.waracle.social_api.entity.RevokedAccessToken;
import com.waracle.social_api.repository.RevokedAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RevokedAccessTokenService {

    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Transactional
    public void revoke(String jti, LocalDateTime expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }

        if (revokedAccessTokenRepository.existsById(jti)) {
            return;
        }

        RevokedAccessToken revoked = new RevokedAccessToken();
        revoked.setJti(jti);
        revoked.setExpiresAt(expiresAt);
        revoked.setRevokedAt(LocalDateTime.now());
        revokedAccessTokenRepository.save(revoked);
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return jti != null && revokedAccessTokenRepository.existsById(jti);
    }

    @Transactional
    public int purgeExpired() {
        return revokedAccessTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
