package com.waracle.social_api.service.feed;

import com.waracle.social_api.dto.response.FeedResponse;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.entity.Post;
import com.waracle.social_api.repository.PostRepository;
import com.waracle.social_api.service.post.PostResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final PostRepository postRepository;
    private final PostResponseMapper postResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long cursor, int limit) {
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Post> posts = cursor == null
                ? postRepository.findByDeletedFalseOrderByIdDesc(pageable)
                : postRepository.findByDeletedFalseAndIdLessThanOrderByIdDesc(cursor, pageable);

        boolean hasMore = posts.size() > pageSize;
        List<Post> page = hasMore ? posts.subList(0, pageSize) : new ArrayList<>(posts);

        List<Long> postIds = page.stream().map(Post::getId).toList();
        Map<Long, List<UserSummaryResponse>> likedByMap = postResponseMapper.loadLikedByForPosts(postIds);

        List<PostResponse> postResponses = page.stream()
                .map(post -> postResponseMapper.toResponse(
                        post,
                        likedByMap.getOrDefault(post.getId(), Collections.emptyList())))
                .toList();

        Long nextCursor = hasMore && !page.isEmpty()
                ? page.getLast().getId()
                : null;

        return new FeedResponse(postResponses, nextCursor, hasMore);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
