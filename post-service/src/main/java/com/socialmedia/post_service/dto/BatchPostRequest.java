package com.socialmedia.post_service.dto;

import java.util.List;

public record BatchPostRequest(List<String> postIds, String requesterId) {}
