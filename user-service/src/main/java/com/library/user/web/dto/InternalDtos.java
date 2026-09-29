package com.library.user.web.dto;

import com.library.user.domain.User;

import java.util.UUID;

/** Ответы внутреннего API. */
public final class InternalDtos {

    public record UserStatusResponse(UUID userId, String status, boolean canBorrow) {

        public static UserStatusResponse from(User reader) {
            return new UserStatusResponse(reader.getId(), reader.getStatus().name(),
                    reader.getStatus() == User.Status.ACTIVE);
        }
    }

    public record UserContactResponse(UUID userId, String fullName, String email, String phone) {

        public static UserContactResponse from(User reader) {
            return new UserContactResponse(reader.getId(), reader.getFullName(),
                    reader.getEmail(), reader.getPhone());
        }
    }

    private InternalDtos() {
    }
}
