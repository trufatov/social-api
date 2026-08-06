package com.waracle.social_api.scheduler;

import com.waracle.social_api.service.auth.RefreshTokenService;
import com.waracle.social_api.service.auth.RevokedAccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RevokedAccessTokenService revokedAccessTokenService;

    @InjectMocks
    private RefreshTokenCleanupScheduler refreshTokenCleanupScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenCleanupScheduler, "retentionDays", 30);
    }

    @Test
    void scheduledRun_cleanup_invokesTokenPurgeService() {
        when(refreshTokenService.purgeExpiredTokens(30)).thenReturn(5);
        when(revokedAccessTokenService.purgeExpired()).thenReturn(2);

        refreshTokenCleanupScheduler.cleanup();

        verify(refreshTokenService).purgeExpiredTokens(30);
        verify(revokedAccessTokenService).purgeExpired();
    }
}
