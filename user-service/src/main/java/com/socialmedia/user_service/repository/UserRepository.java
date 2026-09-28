package com.socialmedia.user_service.repository;

import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    List<User> findByIdNot(UUID id, Pageable pageable);

    @Query("SELECT u.followerCount FROM User u WHERE u.id = :id")
    Optional<Long> findFollowerCountById(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.followerCount = u.followerCount + 1 WHERE u.id = :id")
    void incrementFollowerCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.followerCount = u.followerCount - 1 WHERE u.id = :id AND u.followerCount > 0")
    void decrementFollowerCount(@Param("id") UUID id);
}