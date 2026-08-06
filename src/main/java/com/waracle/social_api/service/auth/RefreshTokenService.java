package com.waracle.social_api.service.auth;

import com.waracle.social_api.entity.RefreshToken;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.token.InvalidRefreshTokenException;
import com.waracle.social_api.exception.token.RefreshTokenReuseException;
import com.waracle.social_api.repository.RefreshTokenRepository;
import com.waracle.social_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();
        saveToken(user, rawToken);
        return rawToken;
    }

    @Transactional
    public TokenRotationResult rotateRefreshToken(String rawToken) {
        RefreshToken stored = findStoredToken(rawToken);

        if (stored.getRevokedAt() != null) {
            revokeAllActiveTokens(stored.getUser().getId());
            throw new RefreshTokenReuseException(
                    "Refresh token reuse detected. All active sessions have been revoked.");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(stored);
            throw new InvalidRefreshTokenException("Refresh token expired.");
        }

        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUser().getId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found."));

        String newRawToken = generateRawToken();
        saveToken(user, newRawToken);
        return new TokenRotationResult(user, newRawToken);
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    @Transactional
    public int purgeExpiredTokens(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
    }

    private RefreshToken findStoredToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token missing.");
        }

        return refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token."));
    }

    private String saveToken(User user, String rawToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private void revokeAllActiveTokens(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available.", ex);
        }
    }
}
