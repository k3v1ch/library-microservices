package com.library.readers.web;

import com.library.readers.service.ReaderService;
import com.library.readers.web.dto.InternalDtos.ReaderContactResponse;
import com.library.readers.web.dto.InternalDtos.ReaderStatusResponse;
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
@RequestMapping("/internal/v1/readers")
public class InternalReaderController {

    private final ReaderService readerService;

    public InternalReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    @Operation(summary = "Статус читателя: можно ли выдавать книги (для circulation-service)")
    @GetMapping("/{id}")
    public ReaderStatusResponse status(@PathVariable UUID id) {
        return ReaderStatusResponse.from(readerService.require(id));
    }

    @Operation(summary = "Читатель по идентификатору учётной записи (claim sub)")
    @GetMapping("/by-user/{userId}")
    public ReaderStatusResponse byUser(@PathVariable UUID userId) {
        return ReaderStatusResponse.from(readerService.requireByUserId(userId));
    }

    @Operation(summary = "Контакты читателя (для notification-service)")
    @GetMapping("/{id}/contact")
    public ReaderContactResponse contact(@PathVariable UUID id) {
        return ReaderContactResponse.from(readerService.require(id));
    }
}
