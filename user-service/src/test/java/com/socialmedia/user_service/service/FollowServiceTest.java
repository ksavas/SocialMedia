package com.socialmedia.user_service.service;

import tools.jackson.databind.ObjectMapper;
import com.socialmedia.user_service.dto.UserProfileResponse;
import com.socialmedia.user_service.dto.UserSummary;
import com.socialmedia.user_service.entity.Follow;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.repository.FollowRepository;
import com.socialmedia.user_service.repository.OutboxEventRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    FollowService followService;

    UUID currentUserId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    User currentUser;
    User targetUser;

    @BeforeEach
    void setUp() {
        followService = new FollowService(followRepository, userRepository, outboxEventRepository, new ObjectMapper());

        currentUser = User.builder().id(currentUserId).username("current").email("c@c.com").password("p").build();
        targetUser = User.builder().id(targetUserId).username("target").email("t@t.com").password("p").build();
    }

    @Test
    void follow_shouldSaveFollowAndOutboxEvent() {
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        followService.follow("current", targetUserId);

        verify(followRepository).save(any(Follow.class));
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("user.followed")));
    }

    @Test
    void follow_shouldThrow_whenAlreadyFollowing() {
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        assertThatThrownBy(() -> followService.follow("current", targetUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already following");
    }

    @Test
    void follow_shouldThrow_whenFollowingSelf() {
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> followService.follow("current", currentUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot follow yourself");
    }

    @Test
    void unfollow_shouldDeleteFollowAndSaveOutboxEvent() {
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        followService.unfollow("current", targetUserId);

        verify(followRepository).deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("user.unfollowed")));
    }

    @Test
    void unfollow_shouldThrow_whenNotFollowing() {
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        assertThatThrownBy(() -> followService.unfollow("current", targetUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not following");
    }

    @Test
    void getProfile_shouldReturnProfileWithCountsAndFollowingFlag() {
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.countByFollowingId(targetUserId)).thenReturn(42L);
        when(followRepository.countByFollowerId(targetUserId)).thenReturn(10L);
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        UserProfileResponse profile = followService.getProfile("current", targetUserId);

        assertThat(profile.id()).isEqualTo(targetUserId);
        assertThat(profile.username()).isEqualTo("target");
        assertThat(profile.followerCount()).isEqualTo(42L);
        assertThat(profile.followingCount()).isEqualTo(10L);
        assertThat(profile.isFollowing()).isTrue();
    }

    @Test
    void getUsers_shouldReturnAllUsersExceptCurrentWithFollowingFlag() {
        UUID otherId = UUID.randomUUID();
        User other = User.builder().id(otherId).username("other").email("o@o.com").password("p").build();

        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll()).thenReturn(List.of(currentUser, other, targetUser));
        when(followRepository.findFollowingIdsByFollowerId(currentUserId)).thenReturn(List.of(targetUserId));

        List<UserSummary> users = followService.getUsers("current");

        assertThat(users).extracting(UserSummary::username).containsExactly("other", "target");
        assertThat(users).extracting(UserSummary::isFollowing).containsExactly(false, true);
        verify(followRepository, never()).existsByFollowerIdAndFollowingId(any(), any());
    }
}
