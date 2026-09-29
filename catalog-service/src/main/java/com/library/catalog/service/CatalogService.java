package com.library.catalog.service;

import com.library.catalog.domain.Book;
import com.library.catalog.domain.BookRepository;
import com.library.catalog.domain.BookSpecifications;
import com.library.common.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final BookRepository books;

    public CatalogService(BookRepository books) {
        this.books = books;
    }

    @Transactional
    public Book create(String isbn, String title, String author, String genre, Integer year, int totalCopies) {
        if (books.existsByIsbn(isbn)) {
            throw ApiException.conflict("isbn_exists", "Книга с таким ISBN уже есть в каталоге");
        }
        Book book = books.save(new Book(isbn, title, author, genre, year, totalCopies));
        log.info("Добавлена книга id={} isbn={} экземпляров={}", book.getId(), isbn, totalCopies);
        return book;
    }

    @Transactional
    public Book update(UUID id, String title, String author, String genre, Integer year, int totalCopies) {
        Book book = require(id);
        book.update(title, author, genre, year, totalCopies);
        return book;
    }

    @Transactional(readOnly = true)
    public Book require(UUID id) {
        return books.findById(id)
                .orElseThrow(() -> ApiException.notFound("book_not_found", "Книга не найдена: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Book> search(String query, String genre, Boolean onlyAvailable, Pageable pageable) {
        return books.findAll(BookSpecifications.matching(query, genre, onlyAvailable), pageable);
    }
}
