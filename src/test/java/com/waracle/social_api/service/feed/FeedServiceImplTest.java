package com.waracle.social_api.service.feed;

import com.waracle.social_api.dto.response.FeedResponse;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.entity.Post;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.repository.PostRepository;
import com.waracle.social_api.service.post.PostResponseMapper;
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostResponseMapper postResponseMapper;

    @InjectMocks
    private FeedServiceImpl feedService;

    @Test
    void noCursor_getFeed_returnsFirstPageWithoutHasMore() {
        User author = TestFixtures.user();
        Post post = TestFixtures.post(5L, author, "Feed post");
        PostResponse postResponse = new PostResponse(5L, "Feed post", "Test User", "user@test.com", LocalDateTime.now(), List.of());

        when(postRepository.findByDeletedFalseOrderByIdDesc(any(Pageable.class)))
                .thenReturn(List.of(post));
        when(postResponseMapper.loadLikedByForPosts(List.of(5L))).thenReturn(Map.of());
        when(postResponseMapper.toResponse(post, List.of())).thenReturn(postResponse);

        FeedResponse feed = feedService.getFeed(null, 20);

        assertThat(feed.posts()).hasSize(1);
        assertThat(feed.hasMore()).isFalse();
        assertThat(feed.nextCursor()).isNull();
    }

    @Test
    void cursorWithExtraResults_getFeed_returnsPageWithNextCursor() {
        User author = TestFixtures.user();
        Post post1 = TestFixtures.post(3L, author, "One");
        Post post2 = TestFixtures.post(2L, author, "Two");
        Post post3 = TestFixtures.post(1L, author, "Three");

        when(postRepository.findByDeletedFalseAndIdLessThanOrderByIdDesc(eq(100L), any(Pageable.class)))
                .thenReturn(List.of(post1, post2, post3));
        when(postResponseMapper.loadLikedByForPosts(List.of(3L, 2L))).thenReturn(Map.of());
        when(postResponseMapper.toResponse(any(), any())).thenAnswer(invocation ->
                new PostResponse(((Post) invocation.getArgument(0)).getId(), "x", "n", "e", LocalDateTime.now(), List.of()));

        FeedResponse feed = feedService.getFeed(100L, 2);

        assertThat(feed.posts()).hasSize(2);
        assertThat(feed.hasMore()).isTrue();
        assertThat(feed.nextCursor()).isEqualTo(2L);
    }

    @Test
    void zeroLimit_getFeed_usesDefaultPageSize() {
        when(postRepository.findByDeletedFalseOrderByIdDesc(any(Pageable.class)))
                .thenReturn(List.of());

        FeedResponse feed = feedService.getFeed(null, 0);

        assertThat(feed.posts()).isEmpty();
    }

    @Test
    void excessiveLimit_getFeed_capsAtMaxPageSize() {
        when(postRepository.findByDeletedFalseOrderByIdDesc(any(Pageable.class)))
                .thenReturn(List.of());

        FeedResponse feed = feedService.getFeed(null, 100);

        assertThat(feed.posts()).isEmpty();
    }
}
