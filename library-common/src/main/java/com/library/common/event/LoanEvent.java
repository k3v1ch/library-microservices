package com.library.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Конверт доменного события выдачи. */
public record LoanEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        UUID loanId,
        UUID readerId,
        UUID bookId,
        String bookTitle,
        LocalDate dueDate,
        Integer daysOverdue
) {

    public static LoanEvent of(String type, UUID loanId, UUID readerId, UUID bookId,
                              String bookTitle, LocalDate dueDate, Integer daysOverdue) {
        return new LoanEvent(UUID.randomUUID(), type, Instant.now(), loanId, readerId, bookId,
                bookTitle, dueDate, daysOverdue);
    }
}
