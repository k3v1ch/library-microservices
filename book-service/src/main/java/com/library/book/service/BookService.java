package com.library.book.service;

import com.library.book.domain.Book;
import com.library.book.domain.BookRepository;
import com.library.book.domain.BookSpecifications;
import com.library.common.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository books;

    public BookService(BookRepository books) {
        this.books = books;
    }

    @Transactional
    public Book create(String title, String author) {
        Book book = books.save(new Book(title, author));
        log.info("Добавлена книга id={} «{}»", book.getId(), title);
        return book;
    }

    @Transactional
    public Book update(UUID id, String title, String author) {
        Book book = require(id);
        book.update(title, author);
        return book;
    }

    @Transactional
    public void delete(UUID id) {
        Book book = require(id);
        if (!book.isAvailable()) {
            throw ApiException.conflict("book_on_hands", "Нельзя удалить книгу, которая на руках");
        }
        books.delete(book);
        log.info("Удалена книга id={}", id);
    }

    @Transactional(readOnly = true)
    public Book require(UUID id) {
        return books.findById(id)
                .orElseThrow(() -> ApiException.notFound("book_not_found", "Книга не найдена: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Book> search(String query, Boolean onlyAvailable, Pageable pageable) {
        return books.findAll(BookSpecifications.matching(query, onlyAvailable), pageable);
    }
}
