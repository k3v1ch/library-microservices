package com.library.readers.web.dto;

import com.library.readers.domain.Reader;

import java.util.UUID;

/** Полный профиль: отдаётся самому читателю и библиотекарю. */
public record ReaderResponse(
        UUID id,
        UUID userId,
        String cardNumber,
        String fullName,
        String email,
        String phone,
        String status,
        String blockedReason
) {

    public static ReaderResponse from(Reader reader) {
        return new ReaderResponse(reader.getId(), reader.getUserId(), reader.getCardNumber(),
                reader.getFullName(), reader.getEmail(), reader.getPhone(),
                reader.getStatus().name(), reader.getBlockedReason());
    }
}
