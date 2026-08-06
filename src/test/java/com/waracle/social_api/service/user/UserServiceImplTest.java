package com.waracle.social_api.service.user;

import com.waracle.social_api.dto.request.RegisterRequest;
import com.waracle.social_api.dto.request.UpdateProfileRequest;
import com.waracle.social_api.dto.response.UserInfoResponse;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.profile.EmailAlreadyExistsException;
import com.waracle.social_api.exception.profile.UserNotFoundException;
import com.waracle.social_api.repository.PostLikeRepository;
import com.waracle.social_api.repository.PostRepository;
import com.waracle.social_api.repository.UserRepository;
import com.waracle.social_api.service.UserMapper;
import com.waracle.social_api.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper mapper;

    @Mock
    private ProfilePictureStorageService profilePictureStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void newEmail_register_savesUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("new@test.com", "password123", "New User");
        User mapped = TestFixtures.user();
        mapped.setPassword("password123");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(mapper.convertToEntityFromDTO(request)).thenReturn(mapped);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        userService.register(request);

        verify(userRepository).save(mapped);
        assertThat(mapped.getPassword()).isEqualTo("encoded");
    }

    @Test
    void duplicateEmail_register_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("dup@test.com", "password123", "Dup")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void existingUser_getUserInfo_returnsStatsAndProfile() {
        User user = TestFixtures.user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.countByAuthor_IdAndDeletedFalse(1L)).thenReturn(3L);
        when(postLikeRepository.countByPost_Author_Id(1L)).thenReturn(7L);

        UserInfoResponse response = userService.getUserInfo(user);

        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.totalPosts()).isEqualTo(3L);
        assertThat(response.totalLikes()).isEqualTo(7L);
    }

    @Test
    void missingUser_getUserInfo_throwsUserNotFoundException() {
        User user = TestFixtures.user();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserInfo(user))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void validRequest_updateProfile_updatesNameAndDescription() {
        User user = TestFixtures.user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(postRepository.countByAuthor_IdAndDeletedFalse(1L)).thenReturn(1L);
        when(postLikeRepository.countByPost_Author_Id(1L)).thenReturn(2L);

        UserInfoResponse response = userService.updateProfile(user, new UpdateProfileRequest("Updated", "New bio"));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(user.getDescription()).isEqualTo("New bio");
    }

    @Test
    void validFile_uploadProfilePicture_storesFileAndUpdatesUser() {
        User user = TestFixtures.user();
        MultipartFile file = mock(MultipartFile.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profilePictureStorageService.store(1L, file)).thenReturn("/uploads/profile-pictures/1-new.png");
        when(userRepository.save(user)).thenReturn(user);
        when(postRepository.countByAuthor_IdAndDeletedFalse(1L)).thenReturn(0L);
        when(postLikeRepository.countByPost_Author_Id(1L)).thenReturn(0L);

        UserInfoResponse response = userService.uploadProfilePicture(user, file);

        verify(profilePictureStorageService).deleteIfExists("/uploads/profile-pictures/1.png");
        assertThat(response.profilePicture()).isEqualTo("/uploads/profile-pictures/1-new.png");
    }
}
