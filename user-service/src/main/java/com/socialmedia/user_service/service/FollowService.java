package com.socialmedia.user_service.service;

import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;
import com.socialmedia.user_service.dto.UserProfileResponse;
import com.socialmedia.user_service.dto.UserSummary;
import com.socialmedia.user_service.entity.Follow;
import com.socialmedia.user_service.entity.OutboxEvent;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.repository.FollowRepository;
import com.socialmedia.user_service.repository.OutboxEventRepository;
import com.socialmedia.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void follow(String currentUsername, UUID targetUserId) {
        User currentUser = findByUsername(currentUsername);

        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)) {
            throw new IllegalStateException("Already following");
        }

        followRepository.save(Follow.builder()
                .followerId(currentUser.getId())
                .followingId(targetUserId)
                .build());

        outboxEventRepository.save(buildOutboxEvent("user.followed", currentUser.getId(), targetUserId));
    }

    public void unfollow(String currentUsername, UUID targetUserId) {
        User currentUser = findByUsername(currentUsername);

        if (!followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)) {
            throw new IllegalStateException("Not following");
        }

        followRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), targetUserId);

        outboxEventRepository.save(buildOutboxEvent("user.unfollowed", currentUser.getId(), targetUserId));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String currentUsername, UUID targetUserId) {
        User currentUser = findByUsername(currentUsername);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserProfileResponse(
                target.getId(),
                target.getUsername(),
                target.getDisplayName(),
                followRepository.countByFollowingId(targetUserId),
                followRepository.countByFollowerId(targetUserId),
                followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)
        );
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getUsers(String currentUsername) {
        User currentUser = findByUsername(currentUsername);

        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(u -> new UserSummary(
                        u.getId(),
                        u.getUsername(),
                        u.getDisplayName(),
                        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), u.getId())
                ))
                .toList();
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private OutboxEvent buildOutboxEvent(String eventType, UUID followerId, UUID followingId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "followerId", followerId.toString(),
                    "followingId", followingId.toString()
            ));
            return OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(followerId.toString())
                    .eventType(eventType)
                    .payload(payload)
                    .build();
        } catch (JacksonIOException e) {
            throw new RuntimeException(e);
        }
    }
}
