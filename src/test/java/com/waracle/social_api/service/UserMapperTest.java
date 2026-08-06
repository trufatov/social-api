package com.waracle.social_api.service;

import com.waracle.social_api.dto.request.RegisterRequest;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.validation.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void registerRequest_convertToEntityFromDTO_mapsUserFields() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", "Test User");

        User user = userMapper.convertToEntityFromDTO(request);

        assertThat(user.getEmail()).isEqualTo("user@test.com");
        assertThat(user.getName()).isEqualTo("Test User");
        assertThat(user.getPassword()).isEqualTo("password123");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getCreatedAt()).isNotNull();
    }
}
