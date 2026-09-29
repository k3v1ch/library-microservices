package com.library.catalog.web;

import com.library.catalog.domain.CopyReservation;
import com.library.catalog.service.CatalogService;
import com.library.catalog.service.ReservationService;
import com.library.catalog.web.dto.ReservationResponse;
import com.library.catalog.web.dto.ReserveRequest;
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
@RequestMapping("/internal/v1/reservations")
public class InternalReservationController {

    private final ReservationService reservationService;
    private final CatalogService catalogService;

    public InternalReservationController(ReservationService reservationService, CatalogService catalogService) {
        this.reservationService = reservationService;
        this.catalogService = catalogService;
    }

    @Operation(summary = "Зарезервировать экземпляр под выдачу (идемпотентно по loanId)")
    @PostMapping
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        CopyReservation reservation = reservationService.reserve(request.loanId(), request.bookId());
        return ReservationResponse.from(reservation, catalogService.require(request.bookId()).getTitle());
    }

    @Operation(summary = "Освободить экземпляр: возврат книги или компенсация саги (идемпотентно)")
    @DeleteMapping("/{loanId}")
    public void release(@PathVariable UUID loanId) {
        reservationService.release(loanId);
    }
}
