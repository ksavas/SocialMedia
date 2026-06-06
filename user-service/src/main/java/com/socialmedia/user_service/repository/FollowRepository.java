package com.socialmedia.user_service.repository;

import com.socialmedia.user_service.entity.Follow;
import com.socialmedia.user_service.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    long countByFollowingId(UUID followingId);
    long countByFollowerId(UUID followerId);
}
