package com.library.borrow.service;

import com.library.borrow.client.BookClient;
import com.library.borrow.client.UserClient;
import com.library.borrow.config.BorrowProperties;
import com.library.borrow.domain.BorrowRecord;
import com.library.common.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/** Сага «Выдача книги» — оркестрация без распределённой транзакции. */
@Service
public class BorrowService {

    private static final Logger log = LoggerFactory.getLogger(BorrowService.class);

    private final BorrowStore store;
    private final UserClient userClient;
    private final BookClient bookClient;
    private final BorrowProperties properties;

    public BorrowService(BorrowStore store, UserClient userClient, BookClient bookClient,
                       BorrowProperties properties) {
        this.store = store;
        this.userClient = userClient;
        this.bookClient = bookClient;
        this.properties = properties;
    }

    public BorrowRecord issue(UUID userId, UUID bookId, String idempotencyKey, String actor) {
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<UUID> known = store.borrowIdByIdempotencyKey(idempotencyKey);
            if (known.isPresent()) {
                log.info("Повтор запроса с Idempotency-Key={} — возвращаю выдачу {}", idempotencyKey, known.get());
                return store.require(known.get());
            }
        }

        UserClient.UserStatusView reader = userClient.status(userId);
        if (!reader.canBorrow()) {
            throw ApiException.conflict("user_blocked",
                    "Читателю нельзя выдавать книги, статус: " + reader.status());
        }
        long open = store.countOpenBorrows(userId);
        if (open >= properties.getMaxActiveBorrows()) {
            throw ApiException.conflict("borrow_limit",
                    "У читателя уже %d книг на руках (лимит %d)".formatted(open, properties.getMaxActiveBorrows()));
        }

        BorrowRecord borrow = store.createPending(userId, bookId, actor);
        try {
            BookClient.ReservationView reservation = bookClient.reserveCopy(borrow.getId(), bookId);
            BorrowRecord active = store.activate(borrow.getId(), reservation.bookTitle(), actor);
            if (StringUtils.hasText(idempotencyKey)) {
                store.rememberIdempotencyKey(idempotencyKey, active.getId());
            }
            return active;
        } catch (RuntimeException ex) {
            compensate(borrow.getId(), ex);
            throw ex;
        }
    }

    public BorrowRecord returnBook(UUID borrowId, String actor) {
        // Транзакция закрывает выдачу и кладёт событие в outbox; экземпляр освобождаем после коммита.
        BorrowRecord borrow = store.markReturned(borrowId, actor);
        releaseCopyQuietly(borrowId);
        return borrow;
    }

    public BorrowRecord extend(UUID borrowId, String actor) {
        return store.extend(borrowId, actor);
    }

    public BorrowRecord require(UUID borrowId) {
        return store.require(borrowId);
    }

    /** Компенсация саги: отклонить выдачу и вернуть экземпляр в каталог. */
    private void compensate(UUID borrowId, RuntimeException cause) {
        String reason = cause instanceof ApiException api
                ? api.getMessage()
                : "Техническая ошибка: " + cause.getClass().getSimpleName();
        try {
            store.reject(borrowId, reason, "system");
        } catch (RuntimeException ex) {
            log.error("Не удалось отклонить выдачу borrowId={}: {}", borrowId, ex.toString());
        }
        releaseCopyQuietly(borrowId);
    }

    /** Если каталог недоступен, выдача остаётся с copyReleasePending — добирает планировщик. */
    private void releaseCopyQuietly(UUID borrowId) {
        try {
            bookClient.releaseCopy(borrowId);
            store.markCopyReleased(borrowId);
        } catch (RuntimeException ex) {
            log.error("Экземпляр не освобождён borrowId={}, отдаю фоновой задаче: {}", borrowId, ex.toString());
            store.markCopyReleasePending(borrowId);
        }
    }
}
