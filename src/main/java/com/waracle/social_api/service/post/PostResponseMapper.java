package com.waracle.social_api.service.post;

import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.entity.Post;
import com.waracle.social_api.entity.PostLike;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostResponseMapper {

    private final PostLikeRepository postLikeRepository;

    public PostResponse toResponse(Post post, List<UserSummaryResponse> likedBy) {
        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getAuthor().getName(),
                post.getAuthor().getEmail(),
                post.getCreatedAt(),
                likedBy);
    }

    public PostResponse toResponse(Post post) {
        return toResponse(post, List.of());
    }

    public Map<Long, List<UserSummaryResponse>> loadLikedByForPosts(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        List<PostLike> likes = postLikeRepository.findByPost_IdIn(postIds);

        return likes.stream()
                .collect(Collectors.groupingBy(
                        like -> like.getPost().getId(),
                        Collectors.mapping(like -> toUserSummary(like.getUser()), Collectors.toList())));
    }

    public List<UserSummaryResponse> loadLikedByForPost(Long postId) {
        return loadLikedByForPosts(List.of(postId))
                .getOrDefault(postId, Collections.emptyList());
    }

    public UserSummaryResponse toUserSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt());
    }
}
