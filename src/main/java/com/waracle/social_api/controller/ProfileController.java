package com.waracle.social_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.waracle.social_api.dto.request.UpdateProfileRequest;
import com.waracle.social_api.dto.response.UserInfoResponse;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.service.user.UserService;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserService userService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserInfoResponse getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return userService.getUserInfo(currentUser);
    }

    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserInfoResponse updateCurrentUser(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        return userService.updateProfile(currentUser, request);

    }

    @PostMapping(value = "/me/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public UserInfoResponse uploadProfilePicture(
            @AuthenticationPrincipal User currentUser,
            @RequestPart("file") MultipartFile file) {

        return userService.uploadProfilePicture(currentUser, file);

    }

}


