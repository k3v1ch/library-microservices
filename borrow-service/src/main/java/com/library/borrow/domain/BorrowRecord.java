package com.library.borrow.domain;

import com.library.common.api.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Выдача книги — проекция текущего состояния (read model). */
@Entity
@Table(name = "borrow_records", indexes = {
        @Index(name = "idx_borrows_reader", columnList = "user_id"),
        @Index(name = "idx_borrows_status_due", columnList = "status,due_date")
})
public class BorrowRecord {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    /** Снимок названия на момент выдачи: уведомление не должно ходить за ним в каталог. */
    @Column(name = "book_title", length = 300)
    private String bookTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BorrowStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "borrow_date")
    private LocalDate borrowDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(nullable = false)
    private int extensions;

    @Column(name = "reject_reason", length = 300)
    private String rejectReason;

    /** Экземпляр ещё не освобождён в каталоге: подхватит фоновая задача (гарантия доставки). */
    @Column(name = "copy_release_pending", nullable = false)
    private boolean copyReleasePending;

    @Column(name = "due_soon_notified", nullable = false)
    private boolean dueSoonNotified;

    @Column(name = "overdue_notified", nullable = false)
    private boolean overdueNotified;

    @Version
    private long version;

    protected BorrowRecord() {
    }

    public BorrowRecord(UUID userId, UUID bookId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.bookId = bookId;
        this.status = BorrowStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public void activate(String bookTitle, int borrowPeriodDays) {
        requireStatus(BorrowStatus.PENDING);
        this.bookTitle = bookTitle;
        this.status = BorrowStatus.ACTIVE;
        this.borrowDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(borrowPeriodDays);
    }

    public void reject(String reason) {
        requireStatus(BorrowStatus.PENDING);
        this.status = BorrowStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void extend(int extraDays, int maxExtensions) {
        if (status != BorrowStatus.ACTIVE) {
            throw ApiException.conflict("borrow_not_active", "Продлить можно только активную выдачу");
        }
        if (extensions >= maxExtensions) {
            throw ApiException.conflict("extension_limit",
                    "Продление больше " + maxExtensions + " раз недоступно");
        }
        this.extensions++;
        this.dueDate = this.dueDate.plusDays(extraDays);
        this.dueSoonNotified = false;
    }

    public void markReturned() {
        if (status != BorrowStatus.ACTIVE && status != BorrowStatus.OVERDUE) {
            throw ApiException.conflict("borrow_not_active", "Эта выдача уже закрыта");
        }
        this.status = BorrowStatus.RETURNED;
        this.returnDate = LocalDate.now();
        this.copyReleasePending = true;
    }

    public void markOverdue() {
        if (status == BorrowStatus.ACTIVE) {
            this.status = BorrowStatus.OVERDUE;
        }
        this.overdueNotified = true;
    }

    public void markDueSoonNotified() {
        this.dueSoonNotified = true;
    }

    public void markCopyReleased() {
        this.copyReleasePending = false;
    }

    public void markCopyReleasePending() {
        this.copyReleasePending = true;
    }

    public int daysOverdue(LocalDate today) {
        if (dueDate == null || !today.isAfter(dueDate)) {
            return 0;
        }
        return (int) (today.toEpochDay() - dueDate.toEpochDay());
    }

    private void requireStatus(BorrowStatus expected) {
        if (this.status != expected) {
            throw ApiException.conflict("illegal_borrow_state",
                    "Ожидался статус %s, а выдача в статусе %s".formatted(expected, this.status));
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public BorrowStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public int getExtensions() {
        return extensions;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public boolean isCopyReleasePending() {
        return copyReleasePending;
    }

    public boolean isDueSoonNotified() {
        return dueSoonNotified;
    }

    public boolean isOverdueNotified() {
        return overdueNotified;
    }
}
