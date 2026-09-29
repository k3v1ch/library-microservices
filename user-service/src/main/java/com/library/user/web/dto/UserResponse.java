package com.library.user.web.dto;

import com.library.user.domain.User;

import java.util.UUID;

/** Полный профиль: отдаётся самому читателю и библиотекарю. */
public record UserResponse(
        UUID id,
        UUID userId,
        String cardNumber,
        String fullName,
        String email,
        String phone,
        String status,
        String blockedReason
) {

    public static UserResponse from(User reader) {
        return new UserResponse(reader.getId(), reader.getUserId(), reader.getCardNumber(),
                reader.getFullName(), reader.getEmail(), reader.getPhone(),
                reader.getStatus().name(), reader.getBlockedReason());
    }
}
