package com.waracle.social_api.exception.handler;

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
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void domainExceptionThrown_handlerMethods_returnErrorResponse() {
        assertThat(handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("dup")).errorMessage())
                .isEqualTo("dup");
        assertThat(handler.handleUserNotFound(new UserNotFoundException("missing")).errorMessage())
                .isEqualTo("missing");
        assertThat(handler.handleDisabledAccount(new DisabledException("x")).errorMessage())
                .isEqualTo("Account is not approved yet.");
        assertThat(handler.handleBadCredentials(new BadCredentialsException("x")).errorMessage())
                .isEqualTo("Invalid email or password.");
        assertThat(handler.handleUsernameNotFound(new UsernameNotFoundException("x")).errorMessage())
                .isEqualTo("Invalid email or password.");
        assertThat(handler.handleAccessDenied(new AccessDeniedException("x")).errorMessage())
                .isEqualTo("Access denied.");
        assertThat(handler.handleInvalidRefreshToken(new InvalidRefreshTokenException("bad")).errorMessage())
                .isEqualTo("bad");
        assertThat(handler.handlePostNotFound(new PostNotFoundException("404")).errorMessage())
                .isEqualTo("404");
        assertThat(handler.handlePostAlreadyLiked(new PostAlreadyLikedException("liked")).errorMessage())
                .isEqualTo("liked");
        assertThat(handler.handlePostAlreadyDeleted(new PostAlreadyDeletedException("deleted")).errorMessage())
                .isEqualTo("deleted");
        assertThat(handler.handleInvalidProfilePicture(new InvalidProfilePictureException("pic")).errorMessage())
                .isEqualTo("pic");
        assertThat(handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(1L)).errorMessage())
                .contains("maximum allowed size");
        assertThat(handler.handleMissingServletRequestPart(
                new MissingServletRequestPartException("file")).errorMessage())
                .isEqualTo("Profile picture file is required.");
        assertThat(handler.handlePostNotLiked(new PostNotLikedException("not liked")).errorMessage())
                .isEqualTo("not liked");
        assertThat(handler.handlePostNotDeleted(new PostNotDeletedException("not deleted")).errorMessage())
                .isEqualTo("not deleted");
        assertThat(handler.handlePostRestoreExpired(new PostRestoreExpiredException("expired")).errorMessage())
                .isEqualTo("expired");
        assertThat(handler.handlePostForbidden(new PostForbiddenException("forbidden")).errorMessage())
                .isEqualTo("forbidden");
        assertThat(handler.handleUserAlreadyEnabled(new UserAlreadyEnabledException("enabled")).errorMessage())
                .isEqualTo("enabled");
        assertThat(handler.handleRefreshTokenReuse(new RefreshTokenReuseException("reuse")).errorMessage())
                .isEqualTo("reuse");
        assertThat(handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("bad json", (org.springframework.http.HttpInputMessage) null))
                .errorMessage())
                .isEqualTo("Malformed JSON request body.");
    }

    @Test
    void validationErrors_methodArgumentNotValid_returnsJoinedFieldMessages() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "size must be at least 8"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        assertThat(handler.handleMethodArgumentNotValid(ex).errorMessage())
                .isEqualTo("email: must not be blank; password: size must be at least 8");
    }

    @Test
    void dataIntegrityViolation_duplicateLikeOrEmail_returnsConflictMessage() {
        assertThat(handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key uk_user_post_like")).errorMessage())
                .isEqualTo("Post already liked.");

        assertThat(handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint on email"))
                .errorMessage())
                .isEqualTo("Email already exists.");

        assertThat(handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("other constraint")).errorMessage())
                .isEqualTo("Resource conflict.");
    }
}
