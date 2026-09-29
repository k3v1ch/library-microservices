package com.library.book.web;

import com.library.book.domain.CopyReservation;
import com.library.book.service.BookService;
import com.library.book.service.ReservationService;
import com.library.book.web.dto.ReservationResponse;
import com.library.book.web.dto.ReserveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Внутренний API: scope=internal, через Gateway не публикуется. */
@Tag(name = "Внутренний API: брони экземпляров")
@RestController
@RequestMapping("/internal/reservations")
public class InternalReservationController {

    private final ReservationService reservationService;
    private final BookService bookService;

    public InternalReservationController(ReservationService reservationService, BookService bookService) {
        this.reservationService = reservationService;
        this.bookService = bookService;
    }

    @Operation(summary = "Зарезервировать экземпляр под выдачу (идемпотентно по borrowId)")
    @PostMapping
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        CopyReservation reservation = reservationService.reserve(request.borrowId(), request.bookId());
        return ReservationResponse.from(reservation, bookService.require(request.bookId()).getTitle());
    }

    @Operation(summary = "Освободить экземпляр: возврат книги или компенсация саги (идемпотентно)")
    @DeleteMapping("/{borrowId}")
    public void release(@PathVariable UUID borrowId) {
        reservationService.release(borrowId);
    }
}
