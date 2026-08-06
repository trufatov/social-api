package com.waracle.social_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest
        (@NotBlank @Size(max = 5000) String content) {}
