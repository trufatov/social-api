package com.waracle.social_api.exception.profile;

public class UserAlreadyEnabledException extends RuntimeException {

    public UserAlreadyEnabledException(String message) {
        super(message);
    }
}
