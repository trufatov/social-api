package com.waracle.social_api.dto.response;

import java.util.List;

public record FeedResponse(
        List<PostResponse> posts,
        Long nextCursor,
        boolean hasMore
) {
}
