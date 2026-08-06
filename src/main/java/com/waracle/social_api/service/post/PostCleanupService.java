package com.waracle.social_api.service.post;

import com.waracle.social_api.repository.PostLikeRepository;
import com.waracle.social_api.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCleanupService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    @Value("${post.soft-delete.retention-days:10}")
    private int retentionDays;

    @Transactional
    public int purgeExpiredDeletedPosts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Long> expiredPostIds = postRepository.findExpiredDeletedPostIds(cutoff);

        if (expiredPostIds.isEmpty()) {
            return 0;
        }

        postLikeRepository.deleteByPost_IdIn(expiredPostIds);
        int deletedPosts = postRepository.deleteByIdIn(expiredPostIds);

        log.info("Permanently deleted {} soft-deleted posts older than {} days", deletedPosts, retentionDays);
        return deletedPosts;
    }
}
