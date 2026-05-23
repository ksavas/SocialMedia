package com.socialmedia.user_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleTokenVerifierImpl implements OAuthTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierImpl(@Value("${oauth.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                throw new InvalidTokenException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            return new OAuthUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name"),
                    Boolean.TRUE.equals(payload.getEmailVerified())
            );
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new InvalidTokenException("Failed to verify Google token: " + e.getMessage());
        }
    }
}