package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;

public interface LoginStrategy {
    boolean supports(Provider provider);
    User resolveUser(LoginRequest request);
}
