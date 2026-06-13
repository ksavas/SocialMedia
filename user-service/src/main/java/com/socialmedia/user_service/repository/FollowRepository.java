package com.socialmedia.user_service.repository;

import com.socialmedia.user_service.entity.Follow;
import com.socialmedia.user_service.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    long countByFollowingId(UUID followingId);
    long countByFollowerId(UUID followerId);

    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = :followingId")
    List<UUID> findFollowerIdsByFollowingId(@Param("followingId") UUID followingId);
}
