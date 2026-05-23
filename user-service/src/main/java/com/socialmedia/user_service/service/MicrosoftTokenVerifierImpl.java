package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class MicrosoftTokenVerifierImpl implements OAuthTokenVerifier {

    private final JwtDecoder jwtDecoder;
    private final String clientId;

    public MicrosoftTokenVerifierImpl(
            @Value("${oauth.microsoft.client-id}") String clientId,
            @Value("${oauth.microsoft.jwks-uri}") String jwksUri
    ) {
        this.clientId = clientId;
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }

    @Override
    public Provider getProvider() {
        return Provider.MICROSOFT;
    }

    @Override
    public OAuthUserInfo verify(String idToken) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);

            if (!jwt.getAudience().contains(clientId)) {
                throw new InvalidTokenException("Token audience mismatch");
            }

            String providerId = jwt.getSubject();
            String email = jwt.getClaim("email");
            String name = jwt.getClaim("name");

            boolean emailVerified = true;

            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }

            return new OAuthUserInfo(providerId, email, name, emailVerified);

        } catch (JwtException e) {
            throw new InvalidTokenException("Failed to verify Microsoft token: " + e.getMessage());
        }
    }
}