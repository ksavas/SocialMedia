package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;

public interface OAuthTokenVerifier {
    OAuthUserInfo verify(String idToken);
    Provider getProvider();
}