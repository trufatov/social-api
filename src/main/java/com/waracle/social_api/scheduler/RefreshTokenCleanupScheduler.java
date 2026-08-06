package com.waracle.social_api.scheduler;

import com.waracle.social_api.service.auth.RefreshTokenService;
import com.waracle.social_api.service.auth.RevokedAccessTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;
    private final RevokedAccessTokenService revokedAccessTokenService;

    @Value("${auth.refresh-token.cleanup-retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${auth.refresh-token.cleanup-cron:0 30 3 * * *}")
    public void cleanup() {
        int deleted = refreshTokenService.purgeExpiredTokens(retentionDays);
        if (deleted > 0) {
            log.info("Purged {} expired refresh token records older than {} days", deleted, retentionDays);
        }

        int revokedDeleted = revokedAccessTokenService.purgeExpired();
        if (revokedDeleted > 0) {
            log.info("Purged {} expired revoked access token records", revokedDeleted);
        }
    }
}
