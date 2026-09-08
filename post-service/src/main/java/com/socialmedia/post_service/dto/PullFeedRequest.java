package com.socialmedia.post_service.dto;

import java.util.List;

public record PullFeedRequest(List<String> authorIds, int limit) {}
