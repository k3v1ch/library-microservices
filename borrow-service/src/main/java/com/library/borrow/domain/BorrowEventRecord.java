package com.library.borrow.domain;

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
@Table(name = "borrow_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_borrow_seq", columnNames = {"borrow_id", "seq"}),
        indexes = @Index(name = "idx_borrow_events_borrow", columnList = "borrow_id"))
public class BorrowEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrow_id", nullable = false)
    private UUID borrowId;

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

    protected BorrowEventRecord() {
    }

    public BorrowEventRecord(UUID borrowId, int seq, String type, String payload, String actor) {
        this.borrowId = borrowId;
        this.seq = seq;
        this.type = type;
        this.payload = payload;
        this.actor = actor;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getBorrowId() {
        return borrowId;
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
