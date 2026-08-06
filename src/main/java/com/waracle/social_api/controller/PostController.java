package com.waracle.social_api.controller;

import com.waracle.social_api.dto.request.CreatePostRequest;
import com.waracle.social_api.dto.response.MessageResponse;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.service.post.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(currentUser, request);
    }

    @PostMapping("/{id}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse likePost(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        postService.likePost(currentUser, id);
        return new MessageResponse("Post liked successfully.");
    }

    @DeleteMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikePost(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        postService.unlikePost(currentUser, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        postService.deletePost(currentUser, id);
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse restorePost(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        postService.restorePost(currentUser, id);
        return new MessageResponse("Post restored successfully.");
    }
}
