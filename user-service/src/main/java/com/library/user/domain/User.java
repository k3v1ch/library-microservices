package com.library.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Профиль читателя. */
@Entity
@Table(name = "users")
public class User {

    public enum Status {
        ACTIVE, BLOCKED
    }

    @Id
    private UUID id;

    /** Идентификатор учётной записи (claim sub из JWT) — связь с auth-service. */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "card_number", nullable = false, unique = true, length = 20)
    private String cardNumber;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "blocked_reason", length = 300)
    private String blockedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(UUID userId, String fullName, String email, String phone) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.cardNumber = "LIB-" + this.id.toString().substring(0, 8).toUpperCase();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void updateContacts(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public void block(String reason) {
        this.status = Status.BLOCKED;
        this.blockedReason = reason;
    }

    public void unblock() {
        this.status = Status.ACTIVE;
        this.blockedReason = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Status getStatus() {
        return status;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
