package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.EmailAlreadyRegisteredException;
import com.socialmedia.user_service.exception.InvalidTokenException;
import com.socialmedia.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private OAuthService oAuthService;

    private OAuthUserInfo validGoogleUserInfo;

    @BeforeEach
    void setUp() {
        validGoogleUserInfo = new OAuthUserInfo(
                "google_user_id_123",
                "ahmet@gmail.com",
                "Ahmet Yilmaz",
                true
        );
    }

    @Test
    void googleAuth_shouldCreateNewUser_whenUserDoesNotExist() {
        String idToken = "valid.google.token";
        when(googleTokenVerifier.verify(idToken)).thenReturn(validGoogleUserInfo);
        when(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google_user_id_123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ahmet@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

        AuthResponse response = oAuthService.googleAuth(idToken);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.username()).isEqualTo("ahmet");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("ahmet@gmail.com") &&
                        user.getProvider() == Provider.GOOGLE &&
                        user.getProviderId().equals("google_user_id_123") &&
                        user.getPassword() == null
        ));
    }

    @Test
    void googleAuth_shouldReturnJwt_whenExistingGoogleUser() {
        String idToken = "valid.google.token";
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .username("ahmet")
                .email("ahmet@gmail.com")
                .displayName("Ahmet Yilmaz")
                .provider(Provider.GOOGLE)
                .providerId("google_user_id_123")
                .build();

        when(googleTokenVerifier.verify(idToken)).thenReturn(validGoogleUserInfo);
        when(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google_user_id_123"))
                .thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("jwt.token.here");

        AuthResponse response = oAuthService.googleAuth(idToken);

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.username()).isEqualTo("ahmet");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleAuth_shouldThrowException_whenEmailAlreadyRegisteredAsLocal() {
        String idToken = "valid.google.token";
        User localUser = User.builder()
                .id(UUID.randomUUID())
                .username("ahmet")
                .email("ahmet@gmail.com")
                .password("hashedPassword")
                .provider(Provider.LOCAL)
                .build();

        when(googleTokenVerifier.verify(idToken)).thenReturn(validGoogleUserInfo);
        when(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "google_user_id_123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ahmet@gmail.com")).thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> oAuthService.googleAuth(idToken))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleAuth_shouldThrowException_whenIdTokenIsInvalid() {
        String idToken = "invalid.token";
        when(googleTokenVerifier.verify(idToken))
                .thenThrow(new InvalidTokenException("Invalid Google ID token"));

        assertThatThrownBy(() -> oAuthService.googleAuth(idToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid Google ID token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleAuth_shouldThrowException_whenEmailNotVerified() {
        String idToken = "valid.google.token";
        OAuthUserInfo unverifiedUserInfo = new OAuthUserInfo(
                "google_user_id_123",
                "ahmet@gmail.com",
                "Ahmet Yilmaz",
                false
        );
        when(googleTokenVerifier.verify(idToken)).thenReturn(unverifiedUserInfo);

        assertThatThrownBy(() -> oAuthService.googleAuth(idToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Email not verified");
        verify(userRepository, never()).save(any(User.class));
    }
}