package com.waracle.social_api.service.post;

import com.waracle.social_api.dto.request.CreatePostRequest;
import com.waracle.social_api.dto.response.PostResponse;
import com.waracle.social_api.entity.User;

public interface PostService {

    PostResponse createPost(User currentUser, CreatePostRequest request);

    void likePost(User currentUser, Long postId);

    void unlikePost(User currentUser, Long postId);

    void deletePost(User currentUser, Long postId);

    void restorePost(User currentUser, Long postId);
}
