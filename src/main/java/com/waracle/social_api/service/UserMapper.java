package com.waracle.social_api.service;

import com.waracle.social_api.dto.request.RegisterRequest;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.validation.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper implements Mapper {

    @Override
    public User convertToEntityFromDTO(RegisterRequest registerRequest) {
        User user = new User();
        user.setEmail(registerRequest.email());
        user.setName(registerRequest.name());
        user.setPassword(registerRequest.password());
        user.setRole(Role.USER);
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
