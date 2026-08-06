package com.waracle.social_api.service.admin;

import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.profile.UserAlreadyEnabledException;
import com.waracle.social_api.exception.profile.UserNotFoundException;
import com.waracle.social_api.repository.UserRepository;
import com.waracle.social_api.support.TestFixtures;
import com.waracle.social_api.validation.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void pendingUser_approveUser_enablesUser() {
        User pending = TestFixtures.user(2L, "pending@test.com", "Pending", Role.USER, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(pending));
        when(userRepository.save(pending)).thenReturn(pending);

        adminService.approveUser(2L);

        assertThat(pending.isEnabled()).isTrue();
        verify(userRepository).save(pending);
    }

    @Test
    void missingUser_approveUser_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.approveUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void adminUser_approveUser_throwsUserAlreadyEnabledException() {
        User admin = TestFixtures.user(1L, "admin@test.com", "Admin", Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.approveUser(1L))
                .isInstanceOf(UserAlreadyEnabledException.class);
    }

    @Test
    void alreadyEnabledUser_approveUser_throwsUserAlreadyEnabledException() {
        User enabled = TestFixtures.user(2L, "enabled@test.com", "Enabled", Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(enabled));

        assertThatThrownBy(() -> adminService.approveUser(2L))
                .isInstanceOf(UserAlreadyEnabledException.class);
    }

    @Test
    void mixedPendingUsers_getPendingUsers_returnsOnlyRegularUsers() {
        User pending = TestFixtures.user(2L, "pending@test.com", "Pending", Role.USER, false);
        User admin = TestFixtures.user(1L, "admin@test.com", "Admin", Role.ADMIN, false);
        when(userRepository.findByEnabledFalse()).thenReturn(List.of(pending, admin));

        var result = adminService.getPendingUsers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().email()).isEqualTo("pending@test.com");
    }
}
