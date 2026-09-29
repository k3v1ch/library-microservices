package com.library.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Бронь экземпляра под конкретную выдачу. */
@Entity
@Table(name = "copy_reservations")
public class CopyReservation {

    public enum Status {
        ACTIVE, RELEASED
    }

    @Id
    private UUID id;

    @Column(name = "borrow_id", nullable = false, unique = true)
    private UUID borrowId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    protected CopyReservation() {
    }

    public CopyReservation(UUID borrowId, UUID bookId) {
        this.id = UUID.randomUUID();
        this.borrowId = borrowId;
        this.bookId = bookId;
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
    }

    public boolean release() {
        if (status == Status.RELEASED) {
            return false;
        }
        status = Status.RELEASED;
        releasedAt = Instant.now();
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBorrowId() {
        return borrowId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }
}
