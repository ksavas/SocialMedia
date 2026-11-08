package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.InvalidCredentialsException;
import com.socialmedia.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalLoginStrategyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private LocalLoginStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LocalLoginStrategy(userRepository, passwordEncoder);
    }

    @Test
    void resolveUser_shouldReturnUser_whenValidUsernameAndPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("kaan")
                .email("kaan@test.com")
                .password("hashed")
                .provider(Provider.LOCAL)
                .build();

        LoginRequest request = new LoginRequest(Provider.LOCAL, "kaan", "secret", null, null);
        when(userRepository.findByUsername("kaan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        User result = strategy.resolveUser(request);

        assertThat(result.getUsername()).isEqualTo("kaan");
    }

    @Test
    void resolveUser_shouldReturnUser_whenLoginByEmail() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("kaan")
                .email("kaan@test.com")
                .password("hashed")
                .provider(Provider.LOCAL)
                .build();

        LoginRequest request = new LoginRequest(Provider.LOCAL, "kaan@test.com", "secret", null, null);
        when(userRepository.findByUsername("kaan@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("kaan@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        User result = strategy.resolveUser(request);

        assertThat(result.getEmail()).isEqualTo("kaan@test.com");
    }

    @Test
    void resolveUser_shouldThrow_whenUserNotFound() {
        LoginRequest request = new LoginRequest(Provider.LOCAL, "unknown", "secret", null, null);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.resolveUser(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void resolveUser_shouldThrow_whenPasswordInvalid() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("kaan")
                .password("hashed")
                .provider(Provider.LOCAL)
                .build();

        LoginRequest request = new LoginRequest(Provider.LOCAL, "kaan", "wrong", null, null);
        when(userRepository.findByUsername("kaan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> strategy.resolveUser(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void resolveUser_shouldReturnUser_whenOAuthUserHasSetPassword() {
        User oauthUser = User.builder()
                .id(UUID.randomUUID())
                .username("kaan")
                .password("hashed")
                .provider(Provider.GOOGLE)
                .build();

        LoginRequest request = new LoginRequest(Provider.LOCAL, "kaan", "secret", null, null);
        when(userRepository.findByUsername("kaan")).thenReturn(Optional.of(oauthUser));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        User result = strategy.resolveUser(request);

        assertThat(result.getUsername()).isEqualTo("kaan");
    }
}
