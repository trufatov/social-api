package com.waracle.social_api.service.auth;

import com.waracle.social_api.entity.RefreshToken;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.token.InvalidRefreshTokenException;
import com.waracle.social_api.exception.token.RefreshTokenReuseException;
import com.waracle.social_api.repository.RefreshTokenRepository;
import com.waracle.social_api.repository.UserRepository;
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private String rawToken;
    private String tokenHash;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
        user = TestFixtures.user();
        rawToken = "test-refresh-token-value";
        tokenHash = sha256(rawToken);
    }

    @Test
    void validUser_createRefreshToken_storesHashedToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String created = refreshTokenService.createRefreshToken(user);

        assertThat(created).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(created);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void validActiveToken_rotateRefreshToken_revokesOldAndIssuesNew() {
        RefreshToken stored = TestFixtures.refreshToken(user, tokenHash, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(stored));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenRotationResult result = refreshTokenService.rotateRefreshToken(rawToken);

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void revokedTokenPresentedAgain_rotateRefreshToken_revokesAllSessionsAndThrows() {
        RefreshToken stored = TestFixtures.refreshToken(user, tokenHash, LocalDateTime.now().plusDays(1));
        stored.setRevokedAt(LocalDateTime.now().minusHours(1));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void expiredToken_rotateRefreshToken_throwsInvalidRefreshTokenException() {
        RefreshToken stored = TestFixtures.refreshToken(user, tokenHash, LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(stored)).thenReturn(stored);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void blankToken_rotateRefreshToken_throwsInvalidRefreshTokenException() {
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(" "))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void unknownToken_rotateRefreshToken_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void blankToken_revokeRefreshToken_doesNothing() {
        refreshTokenService.revokeRefreshToken("  ");

        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void activeToken_revokeRefreshToken_setsRevokedAt() {
        RefreshToken stored = TestFixtures.refreshToken(user, tokenHash, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(stored)).thenReturn(stored);

        refreshTokenService.revokeRefreshToken(rawToken);

        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void expiredRecordsExist_purgeExpiredTokens_deletesFromRepository() {
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(4);

        int deleted = refreshTokenService.purgeExpiredTokens(30);

        assertThat(deleted).isEqualTo(4);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
