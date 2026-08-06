package com.waracle.social_api.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieServiceTest {

    private RefreshTokenCookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new RefreshTokenCookieService();
        ReflectionTestUtils.setField(cookieService, "cookieName", "refresh_token");
        ReflectionTestUtils.setField(cookieService, "cookiePath", "/auth");
        ReflectionTestUtils.setField(cookieService, "cookieDomain", "");
        ReflectionTestUtils.setField(cookieService, "cookieSecure", false);
        ReflectionTestUtils.setField(cookieService, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(cookieService, "refreshExpirationMs", 604800000L);
    }

    @Test
    void refreshTokenPresent_setAndClearRefreshTokenCookie_writesAndClearsCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.setRefreshTokenCookie(response, "abc");
        assertThat(response.getHeader("Set-Cookie")).contains("refresh_token=abc");

        cookieService.clearRefreshTokenCookie(response);
        assertThat(response.getHeaders("Set-Cookie").getLast()).contains("Max-Age=0");
    }

    @Test
    void refreshCookiePresent_readRefreshToken_returnsTokenValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "stored-token"));

        assertThat(cookieService.readRefreshToken(request)).isEqualTo("stored-token");
    }

    @Test
    void blankCookieValue_readRefreshToken_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "   "));

        assertThat(cookieService.readRefreshToken(request)).isNull();
    }

    @Test
    void domainConfigured_setRefreshTokenCookie_includesDomainAttribute() {
        ReflectionTestUtils.setField(cookieService, "cookieDomain", ".somedomain.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.setRefreshTokenCookie(response, "abc");

        assertThat(response.getHeader("Set-Cookie")).contains("Domain=.somedomain.com");
    }
}
