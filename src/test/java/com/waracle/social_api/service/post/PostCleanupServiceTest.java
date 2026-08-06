package com.waracle.social_api.service.post;

import com.waracle.social_api.repository.PostLikeRepository;
import com.waracle.social_api.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCleanupServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostCleanupService postCleanupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postCleanupService, "retentionDays", 10);
    }

    @Test
    void noExpiredPosts_purgeExpiredDeletedPosts_returnsZero() {
        when(postRepository.findExpiredDeletedPostIds(any(LocalDateTime.class))).thenReturn(List.of());

        int deleted = postCleanupService.purgeExpiredDeletedPosts();

        assertThat(deleted).isZero();
        verify(postLikeRepository, never()).deleteByPost_IdIn(any());
    }

    @Test
    void expiredPostsExist_purgeExpiredDeletedPosts_deletesLikesThenPosts() {
        when(postRepository.findExpiredDeletedPostIds(any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));
        when(postRepository.deleteByIdIn(List.of(1L, 2L))).thenReturn(2);

        int deleted = postCleanupService.purgeExpiredDeletedPosts();

        assertThat(deleted).isEqualTo(2);
        verify(postLikeRepository).deleteByPost_IdIn(List.of(1L, 2L));
    }
}
