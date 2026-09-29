package com.library.readers.web.dto;

import com.library.readers.domain.Reader;

import java.util.UUID;

/** Ответы внутреннего API. */
public final class InternalDtos {

    public record ReaderStatusResponse(UUID readerId, String status, boolean canBorrow) {

        public static ReaderStatusResponse from(Reader reader) {
            return new ReaderStatusResponse(reader.getId(), reader.getStatus().name(),
                    reader.getStatus() == Reader.Status.ACTIVE);
        }
    }

    public record ReaderContactResponse(UUID readerId, String fullName, String email, String phone) {

        public static ReaderContactResponse from(Reader reader) {
            return new ReaderContactResponse(reader.getId(), reader.getFullName(),
                    reader.getEmail(), reader.getPhone());
        }
    }

    private InternalDtos() {
    }
}
