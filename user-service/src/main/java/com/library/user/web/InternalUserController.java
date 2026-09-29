package com.library.user.web;

import com.library.user.service.UserService;
import com.library.user.web.dto.InternalDtos.UserContactResponse;
import com.library.user.web.dto.InternalDtos.UserStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Внутренний API для других сервисов (scope=internal, через API Gateway недоступен). */
@Tag(name = "Внутренний API: читатели")
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Статус читателя: можно ли выдавать книги (для borrow-service)")
    @GetMapping("/{id}")
    public UserStatusResponse status(@PathVariable UUID id) {
        return UserStatusResponse.from(userService.require(id));
    }

    @Operation(summary = "Читатель по идентификатору учётной записи (claim sub)")
    @GetMapping("/by-user/{userId}")
    public UserStatusResponse byUser(@PathVariable UUID userId) {
        return UserStatusResponse.from(userService.requireByUserId(userId));
    }

    @Operation(summary = "Контакты читателя (для notification-service)")
    @GetMapping("/{id}/contact")
    public UserContactResponse contact(@PathVariable UUID id) {
        return UserContactResponse.from(userService.require(id));
    }
}
