package com.library.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Ключ идемпотентности для POST /borrow. */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String key;

    @Column(name = "borrow_id", nullable = false)
    private UUID borrowId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, UUID borrowId) {
        this.key = key;
        this.borrowId = borrowId;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public UUID getBorrowId() {
        return borrowId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
