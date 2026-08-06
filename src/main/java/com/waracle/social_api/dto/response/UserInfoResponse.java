package com.waracle.social_api.dto.response;

public record UserInfoResponse(
        String name,
        String description,
        String profilePicture,
        long totalPosts,
        long totalLikes
) {
}
