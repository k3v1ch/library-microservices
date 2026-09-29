package com.library.book.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 200) String author
) {
}
