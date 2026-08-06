package com.waracle.social_api.service.post;

import com.waracle.social_api.dto.request.CreatePostRequest;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.entity.Post;
import com.waracle.social_api.entity.PostLike;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.post.PostAlreadyDeletedException;
import com.waracle.social_api.exception.post.PostAlreadyLikedException;
import com.waracle.social_api.exception.post.PostNotDeletedException;
import com.waracle.social_api.exception.post.PostNotFoundException;
import com.waracle.social_api.exception.post.PostNotLikedException;
import com.waracle.social_api.exception.post.PostRestoreExpiredException;
import com.waracle.social_api.exception.profile.UserNotFoundException;
import com.waracle.social_api.repository.PostLikeRepository;
import com.waracle.social_api.repository.PostRepository;
import com.waracle.social_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final PostResponseMapper postResponseMapper;

    @Value("${post.soft-delete.retention-days:10}")
    private int retentionDays;

    @Override
    @Transactional
    public PostResponse createPost(User currentUser, CreatePostRequest request) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content());
        post.setCreatedAt(LocalDateTime.now());

        Post saved = postRepository.save(post);
        return postResponseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void likePost(User currentUser, Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found."));

        if (postLikeRepository.existsByUser_IdAndPost_Id(currentUser.getId(), postId)) {
            throw new PostAlreadyLikedException("Post already liked.");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        PostLike like = new PostLike();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());

        postLikeRepository.save(like);
    }

    @Override
    @Transactional
    public void unlikePost(User currentUser, Long postId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found."));

        if (!postLikeRepository.existsByUser_IdAndPost_Id(currentUser.getId(), postId)) {
            throw new PostNotLikedException("Post not liked.");
        }

        postLikeRepository.deleteByUser_IdAndPost_Id(currentUser.getId(), postId);
    }

    @Override
    @Transactional
    public void deletePost(User currentUser, Long postId) {
        Post post = postRepository.findByIdAndAuthor_Id(postId, currentUser.getId())
                .orElseThrow(() -> new PostNotFoundException("Post not found."));

        if (post.isDeleted()) {
            throw new PostAlreadyDeletedException("Post is already deleted.");
        }

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void restorePost(User currentUser, Long postId) {
        Post post = postRepository.findByIdAndAuthor_Id(postId, currentUser.getId())
                .orElseThrow(() -> new PostNotFoundException("Post not found."));

        if (!post.isDeleted()) {
            throw new PostNotDeletedException("Post is not deleted.");
        }

        if (post.getDeletedAt() == null) {
            throw new PostNotDeletedException("Post cannot be restored.");
        }

        LocalDateTime restoreDeadline = post.getDeletedAt().plusDays(retentionDays);
        if (LocalDateTime.now().isAfter(restoreDeadline)) {
            throw new PostRestoreExpiredException(
                    "Restore window expired. Posts can only be restored within "
                            + retentionDays + " days of deletion.");
        }

        post.setDeleted(false);
        post.setDeletedAt(null);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }
}
