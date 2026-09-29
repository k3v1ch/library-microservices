package com.library.catalog.service;

import com.library.catalog.domain.Book;
import com.library.catalog.domain.BookRepository;
import com.library.catalog.domain.CopyReservation;
import com.library.catalog.domain.CopyReservationRepository;
import com.library.common.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Резервирование экземпляров под выдачу — внутренний API для circulation-service. */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final BookRepository books;
    private final CopyReservationRepository reservations;

    public ReservationService(BookRepository books, CopyReservationRepository reservations) {
        this.books = books;
        this.reservations = reservations;
    }

    @Transactional
    public CopyReservation reserve(UUID loanId, UUID bookId) {
        Book book = books.findWithLockById(bookId)
                .orElseThrow(() -> ApiException.notFound("book_not_found", "Книга не найдена: " + bookId));

        Optional<CopyReservation> existing = reservations.findByLoanId(loanId);
        if (existing.isPresent()) {
            CopyReservation reservation = existing.get();
            if (!reservation.getBookId().equals(bookId)) {
                throw ApiException.conflict("reservation_conflict",
                        "Для выдачи " + loanId + " уже зарезервирована другая книга");
            }
            log.info("Повторный запрос брони loanId={} — возвращаю существующую (идемпотентность)", loanId);
            return reservation;
        }

        book.takeCopy();
        CopyReservation reservation = reservations.save(new CopyReservation(loanId, bookId));
        log.info("Зарезервирован экземпляр bookId={} loanId={} осталось={}",
                bookId, loanId, book.getAvailableCopies());
        return reservation;
    }

    @Transactional
    public void release(UUID loanId) {
        Optional<CopyReservation> found = reservations.findByLoanId(loanId);
        if (found.isEmpty()) {
            // Компенсация саги могла прийти раньше брони или повториться — это не ошибка.
            log.info("Освобождение брони loanId={}: брони нет, ничего не делаю", loanId);
            return;
        }
        CopyReservation reservation = found.get();
        Book book = books.findWithLockById(reservation.getBookId())
                .orElseThrow(() -> ApiException.notFound("book_not_found", "Книга не найдена"));
        if (reservation.release()) {
            book.returnCopy();
            log.info("Экземпляр возвращён в каталог bookId={} loanId={} доступно={}",
                    book.getId(), loanId, book.getAvailableCopies());
        } else {
            log.info("Бронь loanId={} уже была освобождена (идемпотентность)", loanId);
        }
    }
}
