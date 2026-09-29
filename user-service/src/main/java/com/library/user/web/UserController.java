package com.library.user.web;

import com.library.user.domain.User;
import com.library.user.service.UserService;
import com.library.user.web.dto.PageResponse;
import com.library.user.web.dto.UserRequest;
import com.library.user.web.dto.UserResponse;
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
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Создать профиль читателя для текущей учётной записи")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return UserResponse.from(
                userService.register(userId, request.fullName(), request.email(), request.phone()));
    }

    @Operation(summary = "Свой профиль")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.requireByUserId(UUID.fromString(jwt.getSubject())));
    }

    @Operation(summary = "Изменить свои контакты")
    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserRequest request) {
        User reader = userService.requireByUserId(UUID.fromString(jwt.getSubject()));
        return UserResponse.from(userService.updateContacts(reader.getId(),
                request.fullName(), request.email(), request.phone()));
    }

    @Operation(summary = "Профиль читателя (библиотекарь)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public UserResponse byId(@PathVariable UUID id) {
        return UserResponse.from(userService.require(id));
    }

    @Operation(summary = "Список читателей (библиотекарь)")
    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public PageResponse<UserResponse> list(@RequestParam(required = false) User.Status status,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(userService.list(status, pageable), UserResponse::from);
    }

    @Operation(summary = "Заблокировать читателя (библиотекарь)")
    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public UserResponse block(@PathVariable UUID id,
                               @RequestParam @Size(max = 300) String reason) {
        return UserResponse.from(userService.block(id, reason));
    }

    @Operation(summary = "Снять блокировку (библиотекарь)")
    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public UserResponse unblock(@PathVariable UUID id) {
        return UserResponse.from(userService.unblock(id));
    }
}
