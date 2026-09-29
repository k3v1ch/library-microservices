package com.library.circulation.service;

import com.library.circulation.client.CatalogClient;
import com.library.circulation.client.ReadersClient;
import com.library.circulation.config.CirculationProperties;
import com.library.circulation.domain.Loan;
import com.library.common.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/** Сага «Выдача книги» — оркестрация без распределённой транзакции. */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanStore store;
    private final ReadersClient readersClient;
    private final CatalogClient catalogClient;
    private final CirculationProperties properties;

    public LoanService(LoanStore store, ReadersClient readersClient, CatalogClient catalogClient,
                       CirculationProperties properties) {
        this.store = store;
        this.readersClient = readersClient;
        this.catalogClient = catalogClient;
        this.properties = properties;
    }

    public Loan issue(UUID readerId, UUID bookId, String idempotencyKey, String actor) {
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<UUID> known = store.loanIdByIdempotencyKey(idempotencyKey);
            if (known.isPresent()) {
                log.info("Повтор запроса с Idempotency-Key={} — возвращаю выдачу {}", idempotencyKey, known.get());
                return store.require(known.get());
            }
        }

        ReadersClient.ReaderStatusView reader = readersClient.status(readerId);
        if (!reader.canBorrow()) {
            throw ApiException.conflict("reader_blocked",
                    "Читателю нельзя выдавать книги, статус: " + reader.status());
        }
        long open = store.countOpenLoans(readerId);
        if (open >= properties.getMaxActiveLoans()) {
            throw ApiException.conflict("loan_limit",
                    "У читателя уже %d книг на руках (лимит %d)".formatted(open, properties.getMaxActiveLoans()));
        }

        Loan loan = store.createPending(readerId, bookId, actor);
        try {
            CatalogClient.ReservationView reservation = catalogClient.reserveCopy(loan.getId(), bookId);
            Loan active = store.activate(loan.getId(), reservation.bookTitle(), actor);
            if (StringUtils.hasText(idempotencyKey)) {
                store.rememberIdempotencyKey(idempotencyKey, active.getId());
            }
            return active;
        } catch (RuntimeException ex) {
            compensate(loan.getId(), ex);
            throw ex;
        }
    }

    public Loan returnBook(UUID loanId, String actor) {
        // Транзакция закрывает выдачу и кладёт событие в outbox; экземпляр освобождаем после коммита.
        Loan loan = store.markReturned(loanId, actor);
        releaseCopyQuietly(loanId);
        return loan;
    }

    public Loan extend(UUID loanId, String actor) {
        return store.extend(loanId, actor);
    }

    public Loan require(UUID loanId) {
        return store.require(loanId);
    }

    /** Компенсация саги: отклонить выдачу и вернуть экземпляр в каталог. */
    private void compensate(UUID loanId, RuntimeException cause) {
        String reason = cause instanceof ApiException api
                ? api.getMessage()
                : "Техническая ошибка: " + cause.getClass().getSimpleName();
        try {
            store.reject(loanId, reason, "system");
        } catch (RuntimeException ex) {
            log.error("Не удалось отклонить выдачу loanId={}: {}", loanId, ex.toString());
        }
        releaseCopyQuietly(loanId);
    }

    /** Если каталог недоступен, выдача остаётся с copyReleasePending — добирает планировщик. */
    private void releaseCopyQuietly(UUID loanId) {
        try {
            catalogClient.releaseCopy(loanId);
            store.markCopyReleased(loanId);
        } catch (RuntimeException ex) {
            log.error("Экземпляр не освобождён loanId={}, отдаю фоновой задаче: {}", loanId, ex.toString());
            store.markCopyReleasePending(loanId);
        }
    }
}
