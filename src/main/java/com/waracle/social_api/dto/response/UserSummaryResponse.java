package com.waracle.social_api.dto.response;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id, String email, String name, LocalDateTime createdAt) {}
