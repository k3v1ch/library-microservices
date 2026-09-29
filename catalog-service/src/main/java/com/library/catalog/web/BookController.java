package com.library.catalog.web;

import com.library.catalog.service.CatalogService;
import com.library.catalog.web.dto.BookRequest;
import com.library.catalog.web.dto.BookResponse;
import com.library.catalog.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

@Tag(name = "Каталог книг")
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final CatalogService catalogService;

    public BookController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "Поиск книг: подстрока в названии, авторе или ISBN")
    @GetMapping
    public PageResponse<BookResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String genre,
                                             @RequestParam(required = false) Boolean available,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(catalogService.search(q, genre, available, pageable), BookResponse::from);
    }

    @Operation(summary = "Книга по идентификатору")
    @GetMapping("/{id}")
    public BookResponse byId(@PathVariable UUID id) {
        return BookResponse.from(catalogService.require(id));
    }

    @Operation(summary = "Добавить книгу", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return BookResponse.from(catalogService.create(request.isbn(), request.title(), request.author(),
                request.genre(), request.publishedYear(), request.totalCopies()));
    }

    @Operation(summary = "Изменить книгу", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        return BookResponse.from(catalogService.update(id, request.title(), request.author(),
                request.genre(), request.publishedYear(), request.totalCopies()));
    }
}
