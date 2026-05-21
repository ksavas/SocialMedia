package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthUserInfo;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    @Override
    public OAuthUserInfo verify(String idToken) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}