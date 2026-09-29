package com.library.borrow.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BorrowRepository extends JpaRepository<BorrowRecord, UUID> {

    Page<BorrowRecord> findByUserIdOrderByRequestedAtDesc(UUID userId, Pageable pageable);

    Page<BorrowRecord> findByStatus(BorrowStatus status, Pageable pageable);

    long countByUserIdAndStatusIn(UUID userId, List<BorrowStatus> statuses);

    /** Кандидаты на напоминание «срок подходит». */
    List<BorrowRecord> findByStatusAndDueDateLessThanEqualAndDueSoonNotifiedFalse(BorrowStatus status, LocalDate date);

    /** Просроченные, по которым ещё не уведомляли. */
    List<BorrowRecord> findByStatusInAndDueDateBeforeAndOverdueNotifiedFalse(List<BorrowStatus> statuses, LocalDate date);

    /** Зависшие саги: выдача создана, но не завершилась. */
    List<BorrowRecord> findByStatusAndRequestedAtBefore(BorrowStatus status, Instant before);

    /** Возвраты, по которым каталог ещё не подтвердил освобождение экземпляра. */
    List<BorrowRecord> findByCopyReleasePendingTrue();
}
