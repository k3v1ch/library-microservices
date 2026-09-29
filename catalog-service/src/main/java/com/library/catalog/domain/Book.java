package com.library.catalog.domain;

import com.library.common.api.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Книга каталога вместе со счётчиком свободных экземпляров. */
@Entity
@Table(name = "books")
public class Book {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(length = 100)
    private String genre;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    @Column(name = "available_copies", nullable = false)
    private int availableCopies;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Book() {
    }

    public Book(String isbn, String title, String author, String genre, Integer publishedYear, int totalCopies) {
        this.id = UUID.randomUUID();
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publishedYear = publishedYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
        this.createdAt = Instant.now();
    }

    public void update(String title, String author, String genre, Integer publishedYear, int totalCopies) {
        int issued = this.totalCopies - this.availableCopies;
        if (totalCopies < issued) {
            throw ApiException.conflict("copies_in_use",
                    "Нельзя оставить %d экземпляров: %d уже на руках".formatted(totalCopies, issued));
        }
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publishedYear = publishedYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies - issued;
    }

    public void takeCopy() {
        if (availableCopies <= 0) {
            throw ApiException.conflict("no_available_copies", "Свободных экземпляров нет");
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
