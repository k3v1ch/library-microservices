package com.library.circulation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.config.CirculationProperties;
import com.library.circulation.domain.IdempotencyRecord;
import com.library.circulation.domain.IdempotencyRepository;
import com.library.circulation.domain.Loan;
import com.library.circulation.domain.LoanEventRecord;
import com.library.circulation.domain.LoanEventRepository;
import com.library.circulation.domain.LoanRepository;
import com.library.circulation.domain.LoanStatus;
import com.library.circulation.domain.OutboxMessage;
import com.library.circulation.domain.OutboxRepository;
import com.library.common.api.ApiException;
import com.library.common.event.EventType;
import com.library.common.event.LoanEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Транзакционное ядро сервиса выдачи. */
@Service
public class LoanStore {

    private static final Logger log = LoggerFactory.getLogger(LoanStore.class);
    private static final List<LoanStatus> OPEN_STATUSES = List.of(LoanStatus.PENDING, LoanStatus.ACTIVE, LoanStatus.OVERDUE);

    private final LoanRepository loans;
    private final LoanEventRepository events;
    private final OutboxRepository outbox;
    private final IdempotencyRepository idempotency;
    private final CirculationProperties properties;
    private final ObjectMapper objectMapper;

    public LoanStore(LoanRepository loans, LoanEventRepository events, OutboxRepository outbox,
                     IdempotencyRepository idempotency, CirculationProperties properties,
                     ObjectMapper objectMapper) {
        this.loans = loans;
        this.events = events;
        this.outbox = outbox;
        this.idempotency = idempotency;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Loan createPending(UUID readerId, UUID bookId, String actor) {
        Loan loan = loans.save(new Loan(readerId, bookId));
        appendEvent(loan, "REQUESTED", actor, null);
        return loan;
    }

    @Transactional
    public Loan activate(UUID loanId, String bookTitle, String actor) {
        Loan loan = require(loanId);
        loan.activate(bookTitle, properties.getLoanPeriodDays());
        appendEvent(loan, "ISSUED", actor, null);
        publish(EventType.LOAN_ISSUED, loan, null);
        log.info("Книга выдана loanId={} readerId={} срок={}", loanId, loan.getReaderId(), loan.getDueDate());
        return loan;
    }

    @Transactional
    public Loan reject(UUID loanId, String reason, String actor) {
        Loan loan = require(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            log.warn("Откат саги loanId={} невозможен: статус уже {}", loanId, loan.getStatus());
            return loan;
        }
        loan.reject(reason);
        appendEvent(loan, "REJECTED", actor, reason);
        log.warn("Выдача отклонена loanId={} причина={}", loanId, reason);
        return loan;
    }

    @Transactional
    public Loan markReturned(UUID loanId, String actor) {
        Loan loan = require(loanId);
        loan.markReturned();
        appendEvent(loan, "RETURNED", actor, null);
        publish(EventType.LOAN_RETURNED, loan, null);
        log.info("Книга возвращена loanId={}", loanId);
        return loan;
    }

    @Transactional
    public Loan extend(UUID loanId, String actor) {
        Loan loan = require(loanId);
        loan.extend(properties.getLoanPeriodDays(), properties.getMaxExtensions());
        appendEvent(loan, "EXTENDED", actor, null);
        log.info("Выдача продлена loanId={} новый срок={}", loanId, loan.getDueDate());
        return loan;
    }

    @Transactional
    public void markCopyReleased(UUID loanId) {
        loans.findById(loanId).ifPresent(Loan::markCopyReleased);
    }

    @Transactional
    public void markCopyReleasePending(UUID loanId) {
        loans.findById(loanId).ifPresent(Loan::markCopyReleasePending);
    }

    /** Помечает просроченные выдачи и складывает события в outbox. Возвращает число затронутых. */
    @Transactional
    public int flagOverdue() {
        List<Loan> overdue = loans.findByStatusInAndDueDateBeforeAndOverdueNotifiedFalse(
                List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE), LocalDate.now());
        for (Loan loan : overdue) {
            loan.markOverdue();
            appendEvent(loan, "OVERDUE", "system", null);
            publish(EventType.LOAN_OVERDUE, loan, loan.daysOverdue(LocalDate.now()));
        }
        return overdue.size();
    }

    /** Напоминания «срок подходит»: событие уходит один раз на выдачу (после продления — снова). */
    @Transactional
    public int remindDueSoon() {
        LocalDate threshold = LocalDate.now().plusDays(properties.getDueSoonDays());
        List<Loan> dueSoon = loans.findByStatusAndDueDateLessThanEqualAndDueSoonNotifiedFalse(
                LoanStatus.ACTIVE, threshold);
        for (Loan loan : dueSoon) {
            loan.markDueSoonNotified();
            publish(EventType.LOAN_DUE_SOON, loan, null);
        }
        return dueSoon.size();
    }

    @Transactional(readOnly = true)
    public List<Loan> stuckSagas() {
        return loans.findByStatusAndRequestedAtBefore(LoanStatus.PENDING,
                Instant.now().minus(properties.getSagaTimeout()));
    }

    @Transactional(readOnly = true)
    public List<Loan> pendingCopyReleases() {
        return loans.findByCopyReleasePendingTrue();
    }

    @Transactional(readOnly = true)
    public Loan require(UUID loanId) {
        return loans.findById(loanId)
                .orElseThrow(() -> ApiException.notFound("loan_not_found", "Выдача не найдена: " + loanId));
    }

    @Transactional(readOnly = true)
    public long countOpenLoans(UUID readerId) {
        return loans.countByReaderIdAndStatusIn(readerId, OPEN_STATUSES);
    }

    @Transactional(readOnly = true)
    public Page<Loan> byReader(UUID readerId, Pageable pageable) {
        return loans.findByReaderIdOrderByRequestedAtDesc(readerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Loan> byStatus(LoanStatus status, Pageable pageable) {
        return status == null ? loans.findAll(pageable) : loans.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<LoanEventRecord> history(UUID loanId) {
        require(loanId);
        return events.findByLoanIdOrderBySeqAsc(loanId);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> loanIdByIdempotencyKey(String key) {
        return idempotency.findById(key).map(IdempotencyRecord::getLoanId);
    }

    @Transactional
    public void rememberIdempotencyKey(String key, UUID loanId) {
        idempotency.save(new IdempotencyRecord(key, loanId));
    }

    /** Дописывает событие в журнал. UPDATE по журналу не делается никогда. */
    private void appendEvent(Loan loan, String type, String actor, String reason) {
        int seq = events.countByLoanId(loan.getId()) + 1;
        events.save(new LoanEventRecord(loan.getId(), seq, type, snapshot(loan, reason), actor));
    }

    /** Кладёт событие в outbox — в ту же транзакцию, что и изменение выдачи. */
    private void publish(String type, Loan loan, Integer daysOverdue) {
        LoanEvent event = LoanEvent.of(type, loan.getId(), loan.getReaderId(), loan.getBookId(),
                loan.getBookTitle(), loan.getDueDate(), daysOverdue);
        outbox.save(new OutboxMessage(event.eventId(), type, loan.getId(), toJson(event)));
    }

    private String snapshot(Loan loan, String reason) {
        return toJson(new LoanSnapshot(loan.getId(), loan.getReaderId(), loan.getBookId(), loan.getBookTitle(),
                loan.getStatus().name(), loan.getDueDate(), loan.getExtensions(), reason));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось сериализовать событие", ex);
        }
    }

    private record LoanSnapshot(UUID loanId, UUID readerId, UUID bookId, String bookTitle, String status,
                                LocalDate dueDate, int extensions, String reason) {
    }
}
