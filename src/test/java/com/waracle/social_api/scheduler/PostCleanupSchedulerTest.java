package com.waracle.social_api.scheduler;

import com.waracle.social_api.service.post.PostCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCleanupSchedulerTest {

    @Mock
    private PostCleanupService postCleanupService;

    @InjectMocks
    private PostCleanupScheduler postCleanupScheduler;

    @Test
    void scheduledRun_cleanup_invokesPurgeService() {
        when(postCleanupService.purgeExpiredDeletedPosts()).thenReturn(2);

        postCleanupScheduler.cleanup();

        verify(postCleanupService).purgeExpiredDeletedPosts();
    }
}
