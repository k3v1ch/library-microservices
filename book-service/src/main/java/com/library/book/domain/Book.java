package com.library.book.domain;

import com.library.common.api.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book {

    @Id
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    /** Книга свободна или на руках. Меняется только через бронь под конкретную выдачу. */
    @Column(nullable = false)
    private boolean available;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Book() {
    }

    public Book(String title, String author) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.author = author;
        this.available = true;
        this.createdAt = Instant.now();
    }

    public void update(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public void take() {
        if (!available) {
            throw ApiException.conflict("book_not_available", "Книга уже на руках");
        }
        available = false;
    }

    public void release() {
        available = true;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
