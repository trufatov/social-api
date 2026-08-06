package com.waracle.social_api.exception.post;

public class PostAlreadyDeletedException extends RuntimeException {
    public PostAlreadyDeletedException(String message) {
        super(message);
    }
}
