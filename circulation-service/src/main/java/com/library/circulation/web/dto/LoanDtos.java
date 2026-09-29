package com.library.circulation.web.dto;

import com.library.circulation.domain.Loan;
import com.library.circulation.domain.LoanEventRecord;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Контракты REST API сервиса выдачи. */
public final class LoanDtos {

    public record IssueLoanRequest(@NotNull UUID readerId, @NotNull UUID bookId) {
    }

    public record LoanResponse(
            UUID id,
            UUID readerId,
            UUID bookId,
            String bookTitle,
            String status,
            Instant issuedAt,
            LocalDate dueDate,
            Instant returnedAt,
            int extensions,
            int daysOverdue,
            String rejectReason
    ) {

        public static LoanResponse from(Loan loan) {
            return new LoanResponse(loan.getId(), loan.getReaderId(), loan.getBookId(), loan.getBookTitle(),
                    loan.getStatus().name(), loan.getIssuedAt(), loan.getDueDate(), loan.getReturnedAt(),
                    loan.getExtensions(), loan.daysOverdue(LocalDate.now()), loan.getRejectReason());
        }
    }

    /** Строка журнала событий — то, чего нет в обычной таблице статусов: полная история выдачи. */
    public record LoanEventResponse(int seq, String type, Instant occurredAt, String actor, String payload) {

        public static LoanEventResponse from(LoanEventRecord record) {
            return new LoanEventResponse(record.getSeq(), record.getType(), record.getOccurredAt(),
                    record.getActor(), record.getPayload());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    private LoanDtos() {
    }
}
