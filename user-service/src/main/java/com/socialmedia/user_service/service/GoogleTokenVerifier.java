package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthUserInfo;

public interface GoogleTokenVerifier {
    OAuthUserInfo verify(String idToken);
}