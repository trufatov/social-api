package com.waracle.social_api.service;

import com.waracle.social_api.dto.request.RegisterRequest;
import com.waracle.social_api.entity.User;

public interface Mapper {
    User convertToEntityFromDTO(RegisterRequest registerRequest);
}
