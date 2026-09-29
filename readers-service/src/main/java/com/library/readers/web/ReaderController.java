package com.library.readers.web;

import com.library.readers.domain.Reader;
import com.library.readers.service.ReaderService;
import com.library.readers.web.dto.PageResponse;
import com.library.readers.web.dto.ReaderRequest;
import com.library.readers.web.dto.ReaderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

@Tag(name = "Читатели")
@RestController
@RequestMapping("/api/v1/readers")
@SecurityRequirement(name = "bearerAuth")
public class ReaderController {

    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    @Operation(summary = "Создать профиль читателя для текущей учётной записи")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReaderResponse register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReaderRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ReaderResponse.from(
                readerService.register(userId, request.fullName(), request.email(), request.phone()));
    }

    @Operation(summary = "Свой профиль")
    @GetMapping("/me")
    public ReaderResponse me(@AuthenticationPrincipal Jwt jwt) {
        return ReaderResponse.from(readerService.requireByUserId(UUID.fromString(jwt.getSubject())));
    }

    @Operation(summary = "Изменить свои контакты")
    @PutMapping("/me")
    public ReaderResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReaderRequest request) {
        Reader reader = readerService.requireByUserId(UUID.fromString(jwt.getSubject()));
        return ReaderResponse.from(readerService.updateContacts(reader.getId(),
                request.fullName(), request.email(), request.phone()));
    }

    @Operation(summary = "Профиль читателя (библиотекарь)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ReaderResponse byId(@PathVariable UUID id) {
        return ReaderResponse.from(readerService.require(id));
    }

    @Operation(summary = "Список читателей (библиотекарь)")
    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public PageResponse<ReaderResponse> list(@RequestParam(required = false) Reader.Status status,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(readerService.list(status, pageable), ReaderResponse::from);
    }

    @Operation(summary = "Заблокировать читателя (библиотекарь)")
    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ReaderResponse block(@PathVariable UUID id,
                               @RequestParam @Size(max = 300) String reason) {
        return ReaderResponse.from(readerService.block(id, reason));
    }

    @Operation(summary = "Снять блокировку (библиотекарь)")
    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ReaderResponse unblock(@PathVariable UUID id) {
        return ReaderResponse.from(readerService.unblock(id));
    }
}
