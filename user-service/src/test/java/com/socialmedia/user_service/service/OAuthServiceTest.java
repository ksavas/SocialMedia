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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    private OAuthTokenVerifier googleTokenVerifier;

    @Mock
    private OAuthTokenVerifier microsoftTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private OAuthService oAuthService;

    private OAuthUserInfo validGoogleUserInfo;

    @BeforeEach
    void setUp() {
        when(googleTokenVerifier.getProvider()).thenReturn(Provider.GOOGLE);
        when(microsoftTokenVerifier.getProvider()).thenReturn(Provider.MICROSOFT);

        oAuthService = new OAuthService(
                List.of(googleTokenVerifier, microsoftTokenVerifier),
                userRepository,
                jwtService
        );

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

        AuthResponse response = oAuthService.authenticate(Provider.GOOGLE, idToken);

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

        AuthResponse response = oAuthService.authenticate(Provider.GOOGLE, idToken);

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

        assertThatThrownBy(() -> oAuthService.authenticate(Provider.GOOGLE, idToken))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleAuth_shouldThrowException_whenIdTokenIsInvalid() {
        String idToken = "invalid.token";
        when(googleTokenVerifier.verify(idToken))
                .thenThrow(new InvalidTokenException("Invalid Google ID token"));

        assertThatThrownBy(() -> oAuthService.authenticate(Provider.GOOGLE, idToken))
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

        assertThatThrownBy(() -> oAuthService.authenticate(Provider.GOOGLE, idToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Email not verified");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void microsoftAuth_shouldCreateNewUser_whenUserDoesNotExist() {
        String idToken = "valid.microsoft.token";
        OAuthUserInfo microsoftUserInfo = new OAuthUserInfo(
                "microsoft_user_id_456",
                "ahmet@outlook.com",
                "Ahmet Yilmaz",
                true
        );
        when(microsoftTokenVerifier.verify(idToken)).thenReturn(microsoftUserInfo);
        when(userRepository.findByProviderAndProviderId(Provider.MICROSOFT, "microsoft_user_id_456"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ahmet@outlook.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

        AuthResponse response = oAuthService.authenticate(Provider.MICROSOFT, idToken);

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.username()).isEqualTo("ahmet");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("ahmet@outlook.com") &&
                        user.getProvider() == Provider.MICROSOFT &&
                        user.getProviderId().equals("microsoft_user_id_456") &&
                        user.getPassword() == null
        ));
    }

    @Test
    void microsoftAuth_shouldReturnJwt_whenExistingMicrosoftUser() {
        String idToken = "valid.microsoft.token";
        OAuthUserInfo microsoftUserInfo = new OAuthUserInfo(
                "microsoft_user_id_456",
                "ahmet@outlook.com",
                "Ahmet Yilmaz",
                true
        );
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .username("ahmet")
                .email("ahmet@outlook.com")
                .displayName("Ahmet Yilmaz")
                .provider(Provider.MICROSOFT)
                .providerId("microsoft_user_id_456")
                .build();

        when(microsoftTokenVerifier.verify(idToken)).thenReturn(microsoftUserInfo);
        when(userRepository.findByProviderAndProviderId(Provider.MICROSOFT, "microsoft_user_id_456"))
                .thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(existingUser)).thenReturn("jwt.token.here");

        AuthResponse response = oAuthService.authenticate(Provider.MICROSOFT, idToken);

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.username()).isEqualTo("ahmet");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void microsoftAuth_shouldThrowException_whenEmailAlreadyRegisteredAsLocal() {
        String idToken = "valid.microsoft.token";
        OAuthUserInfo microsoftUserInfo = new OAuthUserInfo(
                "microsoft_user_id_456",
                "ahmet@outlook.com",
                "Ahmet Yilmaz",
                true
        );
        User localUser = User.builder()
                .id(UUID.randomUUID())
                .username("ahmet")
                .email("ahmet@outlook.com")
                .password("hashedPassword")
                .provider(Provider.LOCAL)
                .build();

        when(microsoftTokenVerifier.verify(idToken)).thenReturn(microsoftUserInfo);
        when(userRepository.findByProviderAndProviderId(Provider.MICROSOFT, "microsoft_user_id_456"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ahmet@outlook.com")).thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> oAuthService.authenticate(Provider.MICROSOFT, idToken))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void microsoftAuth_shouldThrowException_whenIdTokenIsInvalid() {
        String idToken = "invalid.token";
        when(microsoftTokenVerifier.verify(idToken))
                .thenThrow(new InvalidTokenException("Invalid Microsoft ID token"));

        assertThatThrownBy(() -> oAuthService.authenticate(Provider.MICROSOFT, idToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid Microsoft ID token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void microsoftAuth_shouldThrowException_whenEmailNotVerified() {
        String idToken = "valid.microsoft.token";
        OAuthUserInfo unverifiedUserInfo = new OAuthUserInfo(
                "microsoft_user_id_456",
                "ahmet@outlook.com",
                "Ahmet Yilmaz",
                false
        );
        when(microsoftTokenVerifier.verify(idToken)).thenReturn(unverifiedUserInfo);

        assertThatThrownBy(() -> oAuthService.authenticate(Provider.MICROSOFT, idToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Email not verified");
        verify(userRepository, never()).save(any(User.class));
    }
}