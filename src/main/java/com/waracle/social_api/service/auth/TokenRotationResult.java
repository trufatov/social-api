package com.waracle.social_api.service.auth;

import com.waracle.social_api.entity.User;

public record TokenRotationResult(User user, String refreshToken) {
}
