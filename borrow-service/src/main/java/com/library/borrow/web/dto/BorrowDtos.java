package com.library.borrow.web.dto;

import com.library.borrow.domain.BorrowRecord;
import com.library.borrow.domain.BorrowEventRecord;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Контракты REST API сервиса выдачи. */
public final class BorrowDtos {

    public record BorrowRequest(@NotNull UUID userId, @NotNull UUID bookId) {
    }

    public record BorrowResponse(
            UUID id,
            UUID userId,
            UUID bookId,
            String bookTitle,
            String status,
            LocalDate borrowDate,
            LocalDate dueDate,
            LocalDate returnDate,
            int extensions,
            int daysOverdue,
            String rejectReason
    ) {

        public static BorrowResponse from(BorrowRecord borrow) {
            return new BorrowResponse(borrow.getId(), borrow.getUserId(), borrow.getBookId(), borrow.getBookTitle(),
                    borrow.getStatus().name(), borrow.getBorrowDate(), borrow.getDueDate(), borrow.getReturnDate(),
                    borrow.getExtensions(), borrow.daysOverdue(LocalDate.now()), borrow.getRejectReason());
        }
    }

    /** Строка журнала событий — то, чего нет в обычной таблице статусов: полная история выдачи. */
    public record BorrowEventResponse(int seq, String type, Instant occurredAt, String actor, String payload) {

        public static BorrowEventResponse from(BorrowEventRecord record) {
            return new BorrowEventResponse(record.getSeq(), record.getType(), record.getOccurredAt(),
                    record.getActor(), record.getPayload());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    private BorrowDtos() {
    }
}
