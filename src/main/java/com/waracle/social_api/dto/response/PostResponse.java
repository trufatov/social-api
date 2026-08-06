package com.waracle.social_api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String content,
        String authorName,
        String authorEmail,
        LocalDateTime createdAt,
        List<UserSummaryResponse> likedBy
) {
}
