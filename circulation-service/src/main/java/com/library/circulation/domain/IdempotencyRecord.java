package com.library.circulation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Ключ идемпотентности для POST /api/v1/loans. */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String key;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, UUID loanId) {
        this.key = key;
        this.loanId = loanId;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
