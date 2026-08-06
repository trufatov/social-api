package com.waracle.social_api.security;

import com.waracle.social_api.exception.handler.ErrorResponseWriter;
import com.waracle.social_api.service.JwtService;
import com.waracle.social_api.service.auth.RevokedAccessTokenService;
import com.waracle.social_api.support.TestFixtures;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RevokedAccessTokenService revokedAccessTokenService;

    @Mock
    private ErrorResponseWriter errorResponseWriter;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthHeader_doFilterInternal_continuesChainWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validBearerToken_doFilterInternal_setsAuthentication() throws Exception {
        var user = TestFixtures.user();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("valid-token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(user);
        when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);
        when(jwtService.extractJti("valid-token")).thenReturn("jti-1");
        when(revokedAccessTokenService.isRevoked("jti-1")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);
    }

    @Test
    void revokedBearerToken_doFilterInternal_returns401WithoutContinuingChain() throws Exception {
        var user = TestFixtures.user();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("revoked-token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(user);
        when(jwtService.isTokenValid("revoked-token", user)).thenReturn(true);
        when(jwtService.extractJti("revoked-token")).thenReturn("jti-revoked");
        when(revokedAccessTokenService.isRevoked("jti-revoked")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED.value(), "Authentication required.");
        verifyNoInteractions(filterChain);
    }

    @Test
    void invalidBearerToken_doFilterInternal_returns401WithoutContinuingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("bad-token")).thenThrow(new io.jsonwebtoken.JwtException("bad"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED.value(), "Authentication required.");
        verifyNoInteractions(filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void emptyBearerToken_doFilterInternal_returns401WithoutContinuingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED.value(), "Authentication required.");
        verifyNoInteractions(filterChain);
    }
}
