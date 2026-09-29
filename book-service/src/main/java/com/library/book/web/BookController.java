package com.library.book.web;

import com.library.book.service.BookService;
import com.library.book.web.dto.BookRequest;
import com.library.book.web.dto.BookResponse;
import com.library.book.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Книги")
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "Список и поиск книг по названию или автору")
    @GetMapping
    public PageResponse<BookResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Boolean available,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(bookService.search(q, available, pageable), BookResponse::from);
    }

    @Operation(summary = "Книга по идентификатору")
    @GetMapping("/{id}")
    public BookResponse byId(@PathVariable UUID id) {
        return BookResponse.from(bookService.require(id));
    }

    @Operation(summary = "Добавить книгу", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return BookResponse.from(bookService.create(request.title(), request.author()));
    }

    @Operation(summary = "Изменить книгу", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        return BookResponse.from(bookService.update(id, request.title(), request.author()));
    }

    @Operation(summary = "Удалить книгу", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        bookService.delete(id);
    }
}
