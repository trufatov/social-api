package com.waracle.social_api.controller;

import com.waracle.social_api.dto.request.CreatePostRequest;
import com.waracle.social_api.dto.request.LoginRequest;
import com.waracle.social_api.dto.request.RegisterRequest;
import com.waracle.social_api.dto.request.UpdateProfileRequest;
import com.waracle.social_api.dto.response.FeedResponse;
import com.waracle.social_api.dto.response.LoginResponse;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.dto.response.UserInfoResponse;
import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.service.AuthenticationService;
import com.waracle.social_api.service.admin.AdminService;
import com.waracle.social_api.service.feed.FeedService;
import com.waracle.social_api.service.post.PostService;
import com.waracle.social_api.service.user.UserService;
import com.waracle.social_api.support.TestFixtures;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerSmokeTest {

    @Mock private UserService userService;
    @Mock private AuthenticationService authenticationService;
    @Mock private PostService postService;
    @Mock private FeedService feedService;
    @Mock private AdminService adminService;

    private AuthController authController;
    private ProfileController profileController;
    private PostController postController;
    private FeedController feedController;
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userService, authenticationService);
        profileController = new ProfileController(userService);
        postController = new PostController(postService);
        feedController = new FeedController(feedService);
        adminController = new AdminController(adminService);
    }

    @Test
    void authRequests_authController_delegatesToServices() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        authController.register(new RegisterRequest("a@b.com", "password123", "A"));
        verify(userService).register(any());

        when(authenticationService.login(any(), eq(response))).thenReturn(new LoginResponse("token"));
        assertThat(authController.login(new LoginRequest("a@b.com", "password"), response).accessToken())
                .isEqualTo("token");

        when(authenticationService.refresh(request, response)).thenReturn(new LoginResponse("refreshed"));
        assertThat(authController.refresh(request, response).accessToken()).isEqualTo("refreshed");

        assertThat(authController.logout(request, response).message()).contains("Logged out");
    }

    @Test
    void profileRequests_profileController_delegatesToUserService() {
        User user = TestFixtures.user();
        UserInfoResponse info = new UserInfoResponse("N", "D", null, 1, 2);
        when(userService.getUserInfo(user)).thenReturn(info);
        when(userService.updateProfile(user, new UpdateProfileRequest("N", "D"))).thenReturn(info);
        when(userService.uploadProfilePicture(eq(user), any())).thenReturn(info);

        assertThat(profileController.getCurrentUser(user)).isEqualTo(info);
        assertThat(profileController.updateCurrentUser(user, new UpdateProfileRequest("N", "D"))).isEqualTo(info);
        assertThat(profileController.uploadProfilePicture(user, new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})))
                .isEqualTo(info);
    }

    @Test
    void postRequests_postController_delegatesToPostService() {
        User user = TestFixtures.user();
        PostResponse postResponse = new PostResponse(1L, "c", "n", "e", LocalDateTime.now(), List.of());

        when(postService.createPost(user, new CreatePostRequest("c"))).thenReturn(postResponse);
        assertThat(postController.createPost(user, new CreatePostRequest("c"))).isEqualTo(postResponse);

        assertThat(postController.likePost(user, 1L).message()).contains("liked");
        postController.unlikePost(user, 1L);
        postController.deletePost(user, 1L);
        assertThat(postController.restorePost(user, 1L).message()).contains("restored");
    }

    @Test
    void feedAndAdminRequests_controllers_delegateToServices() {
        FeedResponse feed = new FeedResponse(List.of(), null, false);
        when(feedService.getFeed(10L, 20)).thenReturn(feed);
        assertThat(feedController.getFeed(10L, 20)).isEqualTo(feed);

        UserSummaryResponse summary = new UserSummaryResponse(1L, "a@b.com", "A", LocalDateTime.now());
        when(adminService.getPendingUsers()).thenReturn(List.of(summary));
        assertThat(adminController.getPendingUsers()).containsExactly(summary);

        assertThat(adminController.approveUser(2L).message()).contains("approved");
        verify(adminService).approveUser(2L);
    }
}
