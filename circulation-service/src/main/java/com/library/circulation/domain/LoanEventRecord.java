package com.library.circulation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** Запись журнала событий выдачи (event sourcing). */
@Entity
@Table(name = "loan_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_loan_seq", columnNames = {"loan_id", "seq"}),
        indexes = @Index(name = "idx_loan_events_loan", columnList = "loan_id"))
public class LoanEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(nullable = false)
    private int seq;

    @Column(nullable = false, length = 32)
    private String type;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Кто инициировал изменение: логин библиотекаря, читателя или "system" для фоновых задач. */
    @Column(name = "actor", length = 64)
    private String actor;

    protected LoanEventRecord() {
    }

    public LoanEventRecord(UUID loanId, int seq, String type, String payload, String actor) {
        this.loanId = loanId;
        this.seq = seq;
        this.type = type;
        this.payload = payload;
        this.actor = actor;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public int getSeq() {
        return seq;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }
}
