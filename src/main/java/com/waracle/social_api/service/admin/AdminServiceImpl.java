package com.waracle.social_api.service.admin;

import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.profile.UserAlreadyEnabledException;
import com.waracle.social_api.exception.profile.UserNotFoundException;
import com.waracle.social_api.repository.UserRepository;
import com.waracle.social_api.validation.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));


        if (user.getRole() == Role.ADMIN) {
            throw new UserAlreadyEnabledException("Admin accounts cannot be approved through this endpoint.");
        }

        if (user.isEnabled()) {
            throw new UserAlreadyEnabledException("User is already approved.");
        }

        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getPendingUsers() {
        return userRepository.findByEnabledFalse().stream()
                .filter(user -> user.getRole() == Role.USER)
                .map(user -> new UserSummaryResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getCreatedAt()))
                .toList();
    }
}
