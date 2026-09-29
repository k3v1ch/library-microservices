package com.library.auth.web.dto;

import com.library.auth.domain.UserAccount;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String username, List<String> roles) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRoles());
    }
}
