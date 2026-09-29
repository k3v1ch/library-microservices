package com.library.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Конверт доменного события выдачи. */
public record BorrowEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        UUID borrowId,
        UUID userId,
        UUID bookId,
        String bookTitle,
        LocalDate dueDate,
        Integer daysOverdue
) {

    public static BorrowEvent of(String type, UUID borrowId, UUID userId, UUID bookId,
                              String bookTitle, LocalDate dueDate, Integer daysOverdue) {
        return new BorrowEvent(UUID.randomUUID(), type, Instant.now(), borrowId, userId, bookId,
                bookTitle, dueDate, daysOverdue);
    }
}
