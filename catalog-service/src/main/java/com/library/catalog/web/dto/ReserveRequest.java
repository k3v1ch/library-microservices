package com.library.catalog.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveRequest(@NotNull UUID loanId, @NotNull UUID bookId) {
}
