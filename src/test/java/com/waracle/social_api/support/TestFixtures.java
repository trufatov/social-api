package com.waracle.social_api.support;

import com.waracle.social_api.entity.Post;
import com.waracle.social_api.entity.RefreshToken;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.validation.Role;

import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user() {
        return user(1L, "user@test.com", "Test User", Role.USER, true);
    }

    public static User user(Long id, String email, String name, Role role, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setEnabled(enabled);
        user.setDescription("Bio");
        user.setProfilePicture("/uploads/profile-pictures/1.png");
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        return user;
    }

    public static Post post(Long id, User author, String content) {
        Post post = new Post();
        post.setId(id);
        post.setAuthor(author);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now().minusHours(1));
        post.setDeleted(false);
        return post;
    }

    public static RefreshToken refreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(LocalDateTime.now());
        return token;
    }
}
