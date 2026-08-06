package com.waracle.social_api.exception.handler;

import com.waracle.social_api.dto.response.ErrorResponse;
import com.waracle.social_api.exception.post.PostAlreadyDeletedException;
import com.waracle.social_api.exception.post.PostAlreadyLikedException;
import com.waracle.social_api.exception.post.PostForbiddenException;
import com.waracle.social_api.exception.post.PostNotDeletedException;
import com.waracle.social_api.exception.post.PostNotFoundException;
import com.waracle.social_api.exception.post.PostNotLikedException;
import com.waracle.social_api.exception.post.PostRestoreExpiredException;
import com.waracle.social_api.exception.profile.EmailAlreadyExistsException;
import com.waracle.social_api.exception.profile.InvalidProfilePictureException;
import com.waracle.social_api.exception.profile.UserAlreadyEnabledException;
import com.waracle.social_api.exception.profile.UserNotFoundException;
import com.waracle.social_api.exception.token.InvalidRefreshTokenException;
import com.waracle.social_api.exception.token.RefreshTokenReuseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.security.access.AccessDeniedException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyEnabledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUserAlreadyEnabled(UserAlreadyEnabledException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidProfilePictureException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidProfilePicture(InvalidProfilePictureException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return new ErrorResponse("Profile picture exceeds maximum allowed size.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        if ("file".equals(ex.getRequestPartName())) {
            return new ErrorResponse("Profile picture file is required.");
        }
        return new ErrorResponse("Required request part '" + ex.getRequestPartName() + "' is missing.");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleDisabledAccount(DisabledException ex) {
        return new ErrorResponse("Account is not approved yet.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
        return new ErrorResponse("Invalid email or password.");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUsernameNotFound(UsernameNotFoundException ex) {
        return new ErrorResponse("Invalid email or password.");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleRefreshTokenReuse(RefreshTokenReuseException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponse("Access denied.");
    }

    @ExceptionHandler(PostNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePostNotFound(PostNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PostAlreadyLikedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handlePostAlreadyLiked(PostAlreadyLikedException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PostNotLikedException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePostNotLiked(PostNotLikedException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PostAlreadyDeletedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handlePostAlreadyDeleted(PostAlreadyDeletedException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PostNotDeletedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePostNotDeleted(PostNotDeletedException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PostRestoreExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ErrorResponse handlePostRestoreExpired(PostRestoreExpiredException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PostForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handlePostForbidden(PostForbiddenException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return new ErrorResponse(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return new ErrorResponse("Malformed JSON request body.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause == null) {
            return new ErrorResponse("Resource conflict.");
        }

        if (cause.contains("uk_user_post_like")) {
            return new ErrorResponse("Post already liked.");
        }

        if (cause.contains("email")) {
            return new ErrorResponse("Email already exists.");
        }

        return new ErrorResponse("Resource conflict.");
    }

    private String formatFieldError(FieldError error) {
        String message = error.getDefaultMessage();
        return error.getField() + ": " + (message != null ? message : "invalid");
    }
}
