package com.library.catalog.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Pattern(regexp = "[0-9Xx-]{10,20}", message = "ISBN: 10–20 символов, цифры, X и дефисы")
        String isbn,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 200) String author,
        @Size(max = 100) String genre,
        @Min(1450) @Max(2100) Integer publishedYear,
        @Min(1) @Max(10_000) int totalCopies
) {
}
