package com.library.catalog.web.dto;

import com.library.catalog.domain.CopyReservation;

import java.util.UUID;

/** Ответ о брони. */
public record ReservationResponse(UUID loanId, UUID bookId, String status, String bookTitle) {

    public static ReservationResponse from(CopyReservation reservation, String bookTitle) {
        return new ReservationResponse(reservation.getLoanId(), reservation.getBookId(),
                reservation.getStatus().name(), bookTitle);
    }
}
