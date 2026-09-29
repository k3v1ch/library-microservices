package com.library.book.service;

import com.library.book.domain.Book;
import com.library.book.domain.BookRepository;
import com.library.book.domain.CopyReservation;
import com.library.book.domain.CopyReservationRepository;
import com.library.common.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Бронь книги под выдачу — внутренний API для borrow-service. */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final BookRepository books;
    private final CopyReservationRepository reservations;

    public ReservationService(BookRepository books, CopyReservationRepository reservations) {
        this.books = books;
        this.reservations = reservations;
    }

    /** Идемпотентно по borrowId: повтор после таймаута не занимает книгу второй раз. */
    @Transactional
    public CopyReservation reserve(UUID borrowId, UUID bookId) {
        // Сначала блокировка книги, потом проверка брони — иначе два параллельных повтора разойдутся.
        Book book = books.findWithLockById(bookId)
                .orElseThrow(() -> ApiException.notFound("book_not_found", "Книга не найдена: " + bookId));

        Optional<CopyReservation> existing = reservations.findByBorrowId(borrowId);
        if (existing.isPresent()) {
            CopyReservation reservation = existing.get();
            if (!reservation.getBookId().equals(bookId)) {
                throw ApiException.conflict("reservation_conflict",
                        "Для выдачи " + borrowId + " уже забронирована другая книга");
            }
            log.info("Повторный запрос брони borrowId={} — возвращаю существующую", borrowId);
            return reservation;
        }

        book.take();
        CopyReservation reservation = reservations.save(new CopyReservation(borrowId, bookId));
        log.info("Книга занята bookId={} borrowId={}", bookId, borrowId);
        return reservation;
    }

    @Transactional
    public void release(UUID borrowId) {
        Optional<CopyReservation> found = reservations.findByBorrowId(borrowId);
        if (found.isEmpty()) {
            // Компенсация саги могла прийти раньше брони или повториться — это не ошибка.
            log.info("Освобождение borrowId={}: брони нет, ничего не делаю", borrowId);
            return;
        }
        CopyReservation reservation = found.get();
        Book book = books.findWithLockById(reservation.getBookId())
                .orElseThrow(() -> ApiException.notFound("book_not_found", "Книга не найдена"));
        if (reservation.release()) {
            book.release();
            log.info("Книга снова доступна bookId={} borrowId={}", book.getId(), borrowId);
        } else {
            log.info("Бронь borrowId={} уже была освобождена", borrowId);
        }
    }
}
