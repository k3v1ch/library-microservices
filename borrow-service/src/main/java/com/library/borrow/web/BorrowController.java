package com.library.borrow.web;

import com.library.borrow.client.UserClient;
import com.library.borrow.domain.BorrowRecord;
import com.library.borrow.domain.BorrowStatus;
import com.library.borrow.service.BorrowService;
import com.library.borrow.service.BorrowStore;
import com.library.borrow.web.dto.BorrowDtos.BorrowRequest;
import com.library.borrow.web.dto.BorrowDtos.BorrowEventResponse;
import com.library.borrow.web.dto.BorrowDtos.BorrowResponse;
import com.library.borrow.web.dto.BorrowDtos.PageResponse;
import com.library.common.api.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Выдача книг")
@RestController
@RequestMapping("/borrow")
@SecurityRequirement(name = "bearerAuth")
public class BorrowController {

    private final BorrowService borrowService;
    private final BorrowStore store;
    private final UserClient userClient;

    public BorrowController(BorrowService borrowService, BorrowStore store, UserClient userClient) {
        this.borrowService = borrowService;
        this.store = store;
        this.userClient = userClient;
    }

    @Operation(summary = "Выдать книгу (сага: читатель → бронь экземпляра → выдача)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public BorrowResponse issue(@Valid @RequestBody BorrowRequest request,
                              @Parameter(description = "Ключ идемпотентности: повтор вернёт ту же выдачу")
                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                              @AuthenticationPrincipal Jwt jwt) {
        return BorrowResponse.from(
                borrowService.issue(request.userId(), request.bookId(), idempotencyKey, actor(jwt)));
    }

    @Operation(summary = "Принять возврат книги")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public BorrowResponse returnBook(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return BorrowResponse.from(borrowService.returnBook(id, actor(jwt)));
    }

    @Operation(summary = "Продлить выдачу: библиотекарь — любую, читатель — только свою")
    @PostMapping("/{id}/extend")
    public BorrowResponse extend(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        requireLibrarianOrOwner(id, jwt);
        return BorrowResponse.from(borrowService.extend(id, actor(jwt)));
    }

    @Operation(summary = "Выдача по идентификатору")
    @GetMapping("/{id}")
    public BorrowResponse byId(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        requireLibrarianOrOwner(id, jwt);
        return BorrowResponse.from(store.require(id));
    }

    @Operation(summary = "Свои выдачи (читатель)")
    @GetMapping("/my")
    public PageResponse<BorrowResponse> my(@AuthenticationPrincipal Jwt jwt,
                                         @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = currentUserId(jwt);
        return PageResponse.of(store.byUser(userId, pageable), BorrowResponse::from);
    }

    @Operation(summary = "Все выдачи с фильтром по статусу (библиотекарь)")
    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public PageResponse<BorrowResponse> list(@RequestParam(required = false) BorrowStatus status,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(store.byStatus(status, pageable), BorrowResponse::from);
    }

    @Operation(summary = "История выдачи из журнала событий (аудит)")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public List<BorrowEventResponse> history(@PathVariable UUID id) {
        return store.history(id).stream().map(BorrowEventResponse::from).toList();
    }

    /** Читатель имеет право видеть и продлевать только свои выдачи. */
    private void requireLibrarianOrOwner(UUID borrowId, Jwt jwt) {
        if (hasRole(jwt, "LIBRARIAN")) {
            return;
        }
        BorrowRecord borrow = store.require(borrowId);
        if (!borrow.getUserId().equals(currentUserId(jwt))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "not_your_borrow", "Это выдача другого читателя");
        }
    }

    private UUID currentUserId(Jwt jwt) {
        return userClient.byUserId(UUID.fromString(jwt.getSubject())).userId();
    }

    private boolean hasRole(Jwt jwt, String role) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(role);
    }

    private String actor(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username == null ? jwt.getSubject() : username;
    }
}
