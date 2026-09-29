package com.library.book.web.dto;

import com.library.book.domain.Book;

import java.util.UUID;

public record BookResponse(UUID id, String title, String author, boolean available) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.isAvailable());
    }
}
