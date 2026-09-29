package com.library.book;

import com.library.book.domain.Book;
import com.library.book.service.BookService;
import com.library.book.service.ReservationService;
import com.library.common.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Бронь и освобождение идемпотентны: сага повторяет вызовы. */
@SpringBootTest
class ReservationServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private ReservationService reservationService;

    @Test
    void repeatedReserveWithSameBorrowIdTakesBookOnce() {
        Book book = bookService.create("Чистый код", "Роберт Мартин");
        UUID borrowId = UUID.randomUUID();

        reservationService.reserve(borrowId, book.getId());
        reservationService.reserve(borrowId, book.getId());

        assertThat(bookService.require(book.getId()).isAvailable()).isFalse();
    }

    @Test
    void reserveFailsWhenBookIsAlreadyTaken() {
        Book book = bookService.create("Редкая книга", "Автор");
        reservationService.reserve(UUID.randomUUID(), book.getId());

        assertThatThrownBy(() -> reservationService.reserve(UUID.randomUUID(), book.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("уже на руках");
    }

    @Test
    void releaseMakesBookAvailableAndIsIdempotent() {
        Book book = bookService.create("Рефакторинг", "Мартин Фаулер");
        UUID borrowId = UUID.randomUUID();
        reservationService.reserve(borrowId, book.getId());
        assertThat(bookService.require(book.getId()).isAvailable()).isFalse();

        reservationService.release(borrowId);
        reservationService.release(borrowId);
        reservationService.release(UUID.randomUUID());

        assertThat(bookService.require(book.getId()).isAvailable()).isTrue();
    }
}
