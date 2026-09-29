package com.library.circulation.web;

import com.library.circulation.client.ReadersClient;
import com.library.circulation.domain.Loan;
import com.library.circulation.domain.LoanStatus;
import com.library.circulation.service.LoanService;
import com.library.circulation.service.LoanStore;
import com.library.circulation.web.dto.LoanDtos.IssueLoanRequest;
import com.library.circulation.web.dto.LoanDtos.LoanEventResponse;
import com.library.circulation.web.dto.LoanDtos.LoanResponse;
import com.library.circulation.web.dto.LoanDtos.PageResponse;
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
@RequestMapping("/api/v1/loans")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;
    private final LoanStore store;
    private final ReadersClient readersClient;

    public LoanController(LoanService loanService, LoanStore store, ReadersClient readersClient) {
        this.loanService = loanService;
        this.store = store;
        this.readersClient = readersClient;
    }

    @Operation(summary = "Выдать книгу (сага: читатель → бронь экземпляра → выдача)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public LoanResponse issue(@Valid @RequestBody IssueLoanRequest request,
                              @Parameter(description = "Ключ идемпотентности: повтор вернёт ту же выдачу")
                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                              @AuthenticationPrincipal Jwt jwt) {
        return LoanResponse.from(
                loanService.issue(request.readerId(), request.bookId(), idempotencyKey, actor(jwt)));
    }

    @Operation(summary = "Принять возврат книги")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public LoanResponse returnBook(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return LoanResponse.from(loanService.returnBook(id, actor(jwt)));
    }

    @Operation(summary = "Продлить выдачу: библиотекарь — любую, читатель — только свою")
    @PostMapping("/{id}/extend")
    public LoanResponse extend(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        requireLibrarianOrOwner(id, jwt);
        return LoanResponse.from(loanService.extend(id, actor(jwt)));
    }

    @Operation(summary = "Выдача по идентификатору")
    @GetMapping("/{id}")
    public LoanResponse byId(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        requireLibrarianOrOwner(id, jwt);
        return LoanResponse.from(store.require(id));
    }

    @Operation(summary = "Свои выдачи (читатель)")
    @GetMapping("/my")
    public PageResponse<LoanResponse> my(@AuthenticationPrincipal Jwt jwt,
                                         @PageableDefault(size = 20) Pageable pageable) {
        UUID readerId = currentReaderId(jwt);
        return PageResponse.of(store.byReader(readerId, pageable), LoanResponse::from);
    }

    @Operation(summary = "Все выдачи с фильтром по статусу (библиотекарь)")
    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public PageResponse<LoanResponse> list(@RequestParam(required = false) LoanStatus status,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(store.byStatus(status, pageable), LoanResponse::from);
    }

    @Operation(summary = "История выдачи из журнала событий (аудит)")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public List<LoanEventResponse> history(@PathVariable UUID id) {
        return store.history(id).stream().map(LoanEventResponse::from).toList();
    }

    /** Читатель имеет право видеть и продлевать только свои выдачи. */
    private void requireLibrarianOrOwner(UUID loanId, Jwt jwt) {
        if (hasRole(jwt, "LIBRARIAN")) {
            return;
        }
        Loan loan = store.require(loanId);
        if (!loan.getReaderId().equals(currentReaderId(jwt))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "not_your_loan", "Это выдача другого читателя");
        }
    }

    private UUID currentReaderId(Jwt jwt) {
        return readersClient.byUserId(UUID.fromString(jwt.getSubject())).readerId();
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
