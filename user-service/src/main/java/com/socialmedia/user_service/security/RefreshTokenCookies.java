package com.socialmedia.user_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookies {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final Duration maxAge;
    private final boolean secure;

    public RefreshTokenCookies(@Value("${jwt.refresh-expiration}") Duration maxAge,
                               @Value("${jwt.refresh-cookie-secure:true}") boolean secure) {
        this.maxAge = maxAge;
        this.secure = secure;
    }

    public String create(String refreshToken) {
        return build(refreshToken, maxAge);
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration age) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(age)
                .build()
                .toString();
    }
}
