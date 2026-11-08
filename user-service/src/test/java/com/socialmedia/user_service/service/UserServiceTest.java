package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.dto.UpdateProfileRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.EmailAlreadyRegisteredException;
import com.socialmedia.user_service.exception.UserAlreadyExistsException;
import com.socialmedia.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123", "Test User");
    }

    @Test
    void register_shouldReturnSavedUser_whenValidRequest() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = userService.register(registerRequest);

        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getProvider()).isEqualTo(Provider.LOCAL);
        verify(userRepository).save(any(User.class));
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldEncodePassword() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void updateProfile_shouldUpdateUsername_whenNewUsernameAvailable() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("olduser").email("user@example.com").provider(Provider.LOCAL).password("encoded").build();
        UpdateProfileRequest request = new UpdateProfileRequest("newuser", null, null);

        when(userRepository.findByUsername("olduser")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("newuser", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("new-jwt");

        AuthResponse response = userService.updateProfile("olduser", request);

        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.token()).isEqualTo("new-jwt");
        assertThat(response.provider()).isEqualTo("LOCAL");
    }

    @Test
    void updateProfile_shouldThrow_whenUsernameAlreadyTaken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("olduser").email("user@example.com").provider(Provider.LOCAL).password("encoded").build();
        UpdateProfileRequest request = new UpdateProfileRequest("takenuser", null, null);

        when(userRepository.findByUsername("olduser")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("takenuser", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile("olduser", request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("This username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldUpdateEmail_whenLocalProviderAndEmailAvailable() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("testuser").email("old@example.com").provider(Provider.LOCAL).password("encoded").build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "new@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("new@example.com", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        AuthResponse response = userService.updateProfile("testuser", request);

        assertThat(response.provider()).isEqualTo("LOCAL");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_shouldThrow_whenEmailAlreadyRegistered() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("testuser").email("old@example.com").provider(Provider.LOCAL).password("encoded").build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "taken@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile("testuser", request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("This email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldIgnoreEmail_whenNotLocalProvider() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("testuser").email("user@example.com").provider(Provider.GOOGLE).password("bcrypt-random").build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "newemail@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        userService.updateProfile("testuser", request);

        verify(userRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void updateProfile_shouldUpdatePassword_whenNotLocalProvider() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("testuser").email("user@example.com").provider(Provider.GOOGLE).password("bcrypt-random").build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, "newpassword123", null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword123")).thenReturn("newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        userService.updateProfile("testuser", request);

        verify(passwordEncoder).encode("newpassword123");
    }
}
