package com.socialmedia.user_service.repository;

import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByUsernameAndIdNot(String username, java.util.UUID id);
    boolean existsByEmailAndIdNot(String email, java.util.UUID id);
}