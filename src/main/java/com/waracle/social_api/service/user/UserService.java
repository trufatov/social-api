package com.waracle.social_api.service.user;

import com.waracle.social_api.dto.request.RegisterRequest;
import com.waracle.social_api.dto.request.UpdateProfileRequest;
import com.waracle.social_api.dto.response.UserInfoResponse;
import com.waracle.social_api.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    void register(RegisterRequest registerRequest);

    UserInfoResponse getUserInfo(User currentUser);

    UserInfoResponse updateProfile(User currentUser, UpdateProfileRequest request);

    UserInfoResponse uploadProfilePicture(User currentUser, MultipartFile file);
}
