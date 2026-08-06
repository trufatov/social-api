package com.waracle.social_api.service.post;

import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.entity.Post;
import com.waracle.social_api.entity.PostLike;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.repository.PostLikeRepository;
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostResponseMapperTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostResponseMapper postResponseMapper;

    @Test
    void postWithoutLikes_toResponse_mapsFieldsWithEmptyLikedBy() {
        User author = TestFixtures.user();
        Post post = TestFixtures.post(1L, author, "Content");

        PostResponse response = postResponseMapper.toResponse(post);

        assertThat(response.content()).isEqualTo("Content");
        assertThat(response.likedBy()).isEmpty();
    }

    @Test
    void emptyPostIds_loadLikedByForPosts_returnsEmptyMap() {
        assertThat(postResponseMapper.loadLikedByForPosts(List.of())).isEmpty();
    }

    @Test
    void likesExist_loadLikedByForPosts_groupsByPostId() {
        User liker = TestFixtures.user(2L, "liker@test.com", "Liker", com.waracle.social_api.validation.Role.USER, true);
        User author = TestFixtures.user();
        Post post = TestFixtures.post(10L, author, "Post");

        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(liker);
        like.setCreatedAt(LocalDateTime.now());

        when(postLikeRepository.findByPost_IdIn(List.of(10L))).thenReturn(List.of(like));

        Map<Long, List<UserSummaryResponse>> likedBy = postResponseMapper.loadLikedByForPosts(List.of(10L));

        assertThat(likedBy.get(10L)).hasSize(1);
        assertThat(likedBy.get(10L).getFirst().email()).isEqualTo("liker@test.com");
    }

    @Test
    void likesExist_loadLikedByForPost_returnsLikerSummaries() {
        User liker = TestFixtures.user(2L, "liker@test.com", "Liker", com.waracle.social_api.validation.Role.USER, true);
        User author = TestFixtures.user();
        Post post = TestFixtures.post(10L, author, "Post");

        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(liker);

        when(postLikeRepository.findByPost_IdIn(List.of(10L))).thenReturn(List.of(like));

        assertThat(postResponseMapper.loadLikedByForPost(10L)).hasSize(1);
    }
}
