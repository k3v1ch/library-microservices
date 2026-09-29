package com.library.book.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveRequest(@NotNull UUID borrowId, @NotNull UUID bookId) {
}
