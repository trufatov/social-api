package com.waracle.social_api.service.post;

import com.waracle.social_api.dto.request.CreatePostRequest;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.entity.Post;
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
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostResponseMapper postResponseMapper;

    @InjectMocks
    private PostServiceImpl postService;

    private User currentUser;
    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "retentionDays", 10);
        currentUser = TestFixtures.user();
        author = TestFixtures.user();
        post = TestFixtures.post(10L, author, "Hello world");
    }

    @Test
    void validUserAndContent_createPost_returnsPostResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(postResponseMapper.toResponse(any(Post.class)))
                .thenReturn(new PostResponse(10L, "Hello world", "Test User", "user@test.com", LocalDateTime.now(), java.util.List.of()));

        PostResponse response = postService.createPost(currentUser, new CreatePostRequest("Hello world"));

        assertThat(response.id()).isEqualTo(10L);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void userMissing_createPost_throwsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(currentUser, new CreatePostRequest("Hello")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void activePostAndNoExistingLike_likePost_savesLike() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUser_IdAndPost_Id(1L, 10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        postService.likePost(currentUser, 10L);

        verify(postLikeRepository).save(any());
    }

    @Test
    void postMissing_likePost_throwsPostNotFoundException() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.likePost(currentUser, 10L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void alreadyLiked_likePost_throwsPostAlreadyLikedException() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUser_IdAndPost_Id(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> postService.likePost(currentUser, 10L))
                .isInstanceOf(PostAlreadyLikedException.class);
    }

    @Test
    void existingLike_unlikePost_deletesLike() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUser_IdAndPost_Id(1L, 10L)).thenReturn(true);

        postService.unlikePost(currentUser, 10L);

        verify(postLikeRepository).deleteByUser_IdAndPost_Id(1L, 10L);
    }

    @Test
    void noExistingLike_unlikePost_throwsPostNotLikedException() {
        when(postRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUser_IdAndPost_Id(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> postService.unlikePost(currentUser, 10L))
                .isInstanceOf(PostNotLikedException.class);
    }

    @Test
    void activeOwnedPost_deletePost_marksPostDeleted() {
        when(postRepository.findByIdAndAuthor_Id(10L, 1L)).thenReturn(Optional.of(post));

        postService.deletePost(currentUser, 10L);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void alreadyDeletedPost_deletePost_throwsPostAlreadyDeletedException() {
        post.setDeleted(true);
        when(postRepository.findByIdAndAuthor_Id(10L, 1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(currentUser, 10L))
                .isInstanceOf(PostAlreadyDeletedException.class);
    }

    @Test
    void recentlyDeletedPost_restorePost_clearsDeletedFlags() {
        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now().minusDays(1));
        when(postRepository.findByIdAndAuthor_Id(10L, 1L)).thenReturn(Optional.of(post));

        postService.restorePost(currentUser, 10L);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().getDeletedAt()).isNull();
    }

    @Test
    void activePost_restorePost_throwsPostNotDeletedException() {
        when(postRepository.findByIdAndAuthor_Id(10L, 1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.restorePost(currentUser, 10L))
                .isInstanceOf(PostNotDeletedException.class);
    }

    @Test
    void deletedWithoutTimestamp_restorePost_throwsPostNotDeletedException() {
        post.setDeleted(true);
        post.setDeletedAt(null);
        when(postRepository.findByIdAndAuthor_Id(10L, 1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.restorePost(currentUser, 10L))
                .isInstanceOf(PostNotDeletedException.class)
                .hasMessageContaining("cannot be restored");
    }

    @Test
    void deletionWindowExpired_restorePost_throwsPostRestoreExpiredException() {
        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now().minusDays(11));
        when(postRepository.findByIdAndAuthor_Id(10L, 1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.restorePost(currentUser, 10L))
                .isInstanceOf(PostRestoreExpiredException.class);
    }
}
