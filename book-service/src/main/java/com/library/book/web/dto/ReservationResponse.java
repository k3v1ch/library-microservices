package com.library.book.web.dto;

import com.library.book.domain.CopyReservation;

import java.util.UUID;

/** Ответ о брони. */
public record ReservationResponse(UUID borrowId, UUID bookId, String status, String bookTitle) {

    public static ReservationResponse from(CopyReservation reservation, String bookTitle) {
        return new ReservationResponse(reservation.getBorrowId(), reservation.getBookId(),
                reservation.getStatus().name(), bookTitle);
    }
}
