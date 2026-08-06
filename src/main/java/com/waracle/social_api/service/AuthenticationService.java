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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RevokedAccessTokenService revokedAccessTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final UserRepository userRepository;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()));

        User user = loadUser(authentication);
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        refreshTokenCookieService.setRefreshTokenCookie(response, refreshToken);
        return new LoginResponse(accessToken);
    }

    @Transactional
    public LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = refreshTokenCookieService.readRefreshToken(request);
        if (rawRefreshToken == null) {
            throw new InvalidRefreshTokenException("Refresh token missing.");
        }

        TokenRotationResult rotation = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        String accessToken = jwtService.generateToken(rotation.user());

        refreshTokenCookieService.setRefreshTokenCookie(response, rotation.refreshToken());
        return new LoginResponse(accessToken);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = refreshTokenCookieService.readRefreshToken(request);
        if (rawRefreshToken == null) {
            throw new InvalidRefreshTokenException("Refresh token missing.");
        }

        revokeAccessTokenFromRequest(request);
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
        refreshTokenCookieService.clearRefreshTokenCookie(response);
    }

    private void revokeAccessTokenFromRequest(HttpServletRequest request) {
        String accessToken = extractBearerToken(request);
        if (accessToken == null) {
            return;
        }

        String jti = jwtService.extractJti(accessToken);
        revokedAccessTokenService.revoke(jti, jwtService.extractExpiration(accessToken));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private User loadUser(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found."));
    }
}
