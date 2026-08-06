package com.waracle.social_api.service;

import com.waracle.social_api.entity.User;
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "8f7d9a4b2c6e1f8a3d5b7c9e1a4f6d8c2b9e7f4a1c6d8e3");
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 900000L);
    }

    @Test
    void validUser_generateToken_extractsUsernameAndValidates() {
        User user = TestFixtures.user();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@test.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenForOtherUser_isTokenValid_returnsFalse() {
        User user = TestFixtures.user();
        User other = TestFixtures.user(2L, "other@test.com", "Other", com.waracle.social_api.validation.Role.USER, true);
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }
}
