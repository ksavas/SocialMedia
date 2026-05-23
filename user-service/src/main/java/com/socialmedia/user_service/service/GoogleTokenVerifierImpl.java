package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierImpl implements OAuthTokenVerifier {

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String idToken) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}