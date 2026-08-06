package com.waracle.social_api.service;

import com.waracle.social_api.dto.request.LoginRequest;
import com.waracle.social_api.dto.response.LoginResponse;
import com.waracle.social_api.entity.User;
import com.waracle.social_api.exception.token.InvalidRefreshTokenException;
import com.waracle.social_api.repository.UserRepository;
import com.waracle.social_api.security.RefreshTokenCookieService;
import com.waracle.social_api.service.auth.RefreshTokenService;
import com.waracle.social_api.service.auth.RevokedAccessTokenService;
import com.waracle.social_api.service.auth.TokenRotationResult;
import com.waracle.social_api.support.TestFixtures;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RevokedAccessTokenService revokedAccessTokenService;

    @Mock
    private RefreshTokenCookieService refreshTokenCookieService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void validCredentials_login_returnsAccessTokenAndSetsCookie() {
        User user = TestFixtures.user();
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);
        LoginRequest request = new LoginRequest("user@test.com", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        LoginResponse loginResponse = authenticationService.login(request, response);

        assertThat(loginResponse.accessToken()).isEqualTo("access-token");
        verify(refreshTokenCookieService).setRefreshTokenCookie(response, "refresh-token");
    }

    @Test
    void validRefreshCookie_refresh_returnsNewAccessTokenAndRotatesCookie() {
        User user = TestFixtures.user();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(refreshTokenCookieService.readRefreshToken(request)).thenReturn("raw-refresh");
        when(refreshTokenService.rotateRefreshToken("raw-refresh"))
                .thenReturn(new TokenRotationResult(user, "new-refresh"));
        when(jwtService.generateToken(user)).thenReturn("new-access");

        LoginResponse loginResponse = authenticationService.refresh(request, response);

        assertThat(loginResponse.accessToken()).isEqualTo("new-access");
        verify(refreshTokenCookieService).setRefreshTokenCookie(response, "new-refresh");
    }

    @Test
    void missingRefreshCookie_refresh_throwsInvalidRefreshTokenException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(refreshTokenCookieService.readRefreshToken(request)).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.refresh(request, response))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void activeRefreshCookie_logout_revokesTokenAndClearsCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(refreshTokenCookieService.readRefreshToken(request)).thenReturn("raw-refresh");
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(jwtService.extractJti("access-token")).thenReturn("jti-1");
        when(jwtService.extractExpiration("access-token")).thenReturn(java.time.LocalDateTime.now().plusMinutes(15));

        authenticationService.logout(request, response);

        verify(revokedAccessTokenService).revoke(org.mockito.ArgumentMatchers.eq("jti-1"), org.mockito.ArgumentMatchers.any());
        verify(refreshTokenService).revokeRefreshToken("raw-refresh");
        verify(refreshTokenCookieService).clearRefreshTokenCookie(response);
    }

    @Test
    void missingRefreshCookie_logout_throwsInvalidRefreshTokenException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(refreshTokenCookieService.readRefreshToken(request)).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.logout(request, response))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
