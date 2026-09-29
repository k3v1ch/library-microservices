package com.library.circulation.domain;

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
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_reader", columnList = "reader_id"),
        @Index(name = "idx_loans_status_due", columnList = "status,due_date")
})
public class Loan {

    @Id
    private UUID id;

    @Column(name = "reader_id", nullable = false)
    private UUID readerId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    /** Снимок названия на момент выдачи: уведомление не должно ходить за ним в каталог. */
    @Column(name = "book_title", length = 300)
    private String bookTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LoanStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

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

    protected Loan() {
    }

    public Loan(UUID readerId, UUID bookId) {
        this.id = UUID.randomUUID();
        this.readerId = readerId;
        this.bookId = bookId;
        this.status = LoanStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public void activate(String bookTitle, int loanPeriodDays) {
        requireStatus(LoanStatus.PENDING);
        this.bookTitle = bookTitle;
        this.status = LoanStatus.ACTIVE;
        this.issuedAt = Instant.now();
        this.dueDate = LocalDate.now().plusDays(loanPeriodDays);
    }

    public void reject(String reason) {
        requireStatus(LoanStatus.PENDING);
        this.status = LoanStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void extend(int extraDays, int maxExtensions) {
        if (status != LoanStatus.ACTIVE) {
            throw ApiException.conflict("loan_not_active", "Продлить можно только активную выдачу");
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
        if (status != LoanStatus.ACTIVE && status != LoanStatus.OVERDUE) {
            throw ApiException.conflict("loan_not_active", "Эта выдача уже закрыта");
        }
        this.status = LoanStatus.RETURNED;
        this.returnedAt = Instant.now();
        this.copyReleasePending = true;
    }

    public void markOverdue() {
        if (status == LoanStatus.ACTIVE) {
            this.status = LoanStatus.OVERDUE;
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

    private void requireStatus(LoanStatus expected) {
        if (this.status != expected) {
            throw ApiException.conflict("illegal_loan_state",
                    "Ожидался статус %s, а выдача в статусе %s".formatted(expected, this.status));
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getReaderId() {
        return readerId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getReturnedAt() {
        return returnedAt;
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
