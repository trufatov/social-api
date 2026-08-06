package com.waracle.social_api.scheduler;

import com.waracle.social_api.service.post.PostCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostCleanupScheduler {

    private final PostCleanupService postCleanupService;

    @Scheduled(cron = "${post.cleanup.cron:0 0 3 * * *}")
    public void cleanup() {
        postCleanupService.purgeExpiredDeletedPosts();
    }
}
