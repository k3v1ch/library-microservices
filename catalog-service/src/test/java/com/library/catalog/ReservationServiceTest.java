package com.library.catalog;

import com.library.catalog.domain.Book;
import com.library.catalog.service.CatalogService;
import com.library.catalog.service.ReservationService;
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
    private CatalogService catalogService;

    @Autowired
    private ReservationService reservationService;

    @Test
    void repeatedReserveWithSameLoanIdTakesOnlyOneCopy() {
        Book book = catalogService.create(randomIsbn(), "Чистый код", "Роберт Мартин", "IT", 2008, 2);
        UUID loanId = UUID.randomUUID();

        reservationService.reserve(loanId, book.getId());
        reservationService.reserve(loanId, book.getId());

        assertThat(catalogService.require(book.getId()).getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void reserveFailsWhenNoCopiesLeft() {
        Book book = catalogService.create(randomIsbn(), "Редкая книга", "Автор", "История", 1999, 1);
        reservationService.reserve(UUID.randomUUID(), book.getId());

        assertThatThrownBy(() -> reservationService.reserve(UUID.randomUUID(), book.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Свободных экземпляров нет");
    }

    @Test
    void releaseReturnsCopyAndIsIdempotent() {
        Book book = catalogService.create(randomIsbn(), "Рефакторинг", "Мартин Фаулер", "IT", 2018, 1);
        UUID loanId = UUID.randomUUID();
        reservationService.reserve(loanId, book.getId());
        assertThat(catalogService.require(book.getId()).getAvailableCopies()).isZero();

        reservationService.release(loanId);
        reservationService.release(loanId);
        reservationService.release(UUID.randomUUID());

        assertThat(catalogService.require(book.getId()).getAvailableCopies()).isEqualTo(1);
    }

    private String randomIsbn() {
        return String.valueOf(System.nanoTime()).substring(0, 13);
    }
}
