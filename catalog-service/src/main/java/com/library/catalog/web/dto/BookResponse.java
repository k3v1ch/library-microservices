package com.library.catalog.web.dto;

import com.library.catalog.domain.Book;

import java.util.UUID;

public record BookResponse(
        UUID id,
        String isbn,
        String title,
        String author,
        String genre,
        Integer publishedYear,
        int totalCopies,
        int availableCopies
) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getPublishedYear(), book.getTotalCopies(), book.getAvailableCopies());
    }
}
