package com.waracle.social_api.exception.post;

public class PostAlreadyLikedException extends RuntimeException {

    public PostAlreadyLikedException(String message) {
        super(message);
    }
}
