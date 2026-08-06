package com.waracle.social_api.service.user;

import com.waracle.social_api.repository.UserRepository;
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void existingEmail_loadUserByUsername_returnsUser() {
        var user = TestFixtures.user();
        when(repository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThat(customUserDetailsService.loadUserByUsername("user@test.com")).isEqualTo(user);
    }

    @Test
    void missingEmail_loadUserByUsername_throwsUsernameNotFoundException() {
        when(repository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
