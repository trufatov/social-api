package com.waracle.social_api.service.auth;

import com.waracle.social_api.entity.RevokedAccessToken;
import com.waracle.social_api.repository.RevokedAccessTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokedAccessTokenServiceTest {

    @Mock
    private RevokedAccessTokenRepository revokedAccessTokenRepository;

    @InjectMocks
    private RevokedAccessTokenService revokedAccessTokenService;

    @Test
    void newJti_revoke_persistsRevokedToken() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        when(revokedAccessTokenRepository.existsById("jti-1")).thenReturn(false);

        revokedAccessTokenService.revoke("jti-1", expiresAt);

        ArgumentCaptor<RevokedAccessToken> captor = ArgumentCaptor.forClass(RevokedAccessToken.class);
        verify(revokedAccessTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void existingJti_revoke_skipsDuplicateInsert() {
        when(revokedAccessTokenRepository.existsById("jti-1")).thenReturn(true);

        revokedAccessTokenService.revoke("jti-1", LocalDateTime.now().plusMinutes(15));

        verify(revokedAccessTokenRepository, never()).save(any());
    }

    @Test
    void revokedJti_isRevoked_returnsTrue() {
        when(revokedAccessTokenRepository.existsById("jti-1")).thenReturn(true);

        assertThat(revokedAccessTokenService.isRevoked("jti-1")).isTrue();
    }
}
