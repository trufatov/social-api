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
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@Primary
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final ProfilePictureStorageService profilePictureStorageService;

    @Override
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        User user = mapper.convertToEntityFromDTO(registerRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        return buildUserInfoResponse(user);
    }

    @Override
    @Transactional
    public UserInfoResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.setName(request.name());
        user.setDescription(request.description());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        return buildUserInfoResponse(saved);
    }

    @Override
    @Transactional
    public UserInfoResponse uploadProfilePicture(User currentUser, MultipartFile file) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        profilePictureStorageService.deleteIfExists(user.getProfilePicture());
        String profilePictureUrl = profilePictureStorageService.store(user.getId(), file);

        user.setProfilePicture(profilePictureUrl);
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        return buildUserInfoResponse(saved);
    }

    private UserInfoResponse buildUserInfoResponse(User user) {
        long totalPosts = postRepository.countByAuthor_IdAndDeletedFalse(user.getId());
        long totalLikes = postLikeRepository.countByPost_Author_Id(user.getId());

        return new UserInfoResponse(
                user.getName(),
                user.getDescription(),
                user.getProfilePicture(),
                totalPosts,
                totalLikes);
    }
}
