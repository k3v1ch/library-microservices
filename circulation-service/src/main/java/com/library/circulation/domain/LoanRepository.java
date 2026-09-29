package com.library.circulation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Page<Loan> findByReaderIdOrderByRequestedAtDesc(UUID readerId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    long countByReaderIdAndStatusIn(UUID readerId, List<LoanStatus> statuses);

    /** Кандидаты на напоминание «срок подходит». */
    List<Loan> findByStatusAndDueDateLessThanEqualAndDueSoonNotifiedFalse(LoanStatus status, LocalDate date);

    /** Просроченные, по которым ещё не уведомляли. */
    List<Loan> findByStatusInAndDueDateBeforeAndOverdueNotifiedFalse(List<LoanStatus> statuses, LocalDate date);

    /** Зависшие саги: выдача создана, но не завершилась. */
    List<Loan> findByStatusAndRequestedAtBefore(LoanStatus status, Instant before);

    /** Возвраты, по которым каталог ещё не подтвердил освобождение экземпляра. */
    List<Loan> findByCopyReleasePendingTrue();
}
