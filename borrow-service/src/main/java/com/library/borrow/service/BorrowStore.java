package com.library.borrow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.borrow.config.BorrowProperties;
import com.library.borrow.domain.IdempotencyRecord;
import com.library.borrow.domain.IdempotencyRepository;
import com.library.borrow.domain.BorrowRecord;
import com.library.borrow.domain.BorrowEventRecord;
import com.library.borrow.domain.BorrowEventRepository;
import com.library.borrow.domain.BorrowRepository;
import com.library.borrow.domain.BorrowStatus;
import com.library.borrow.domain.OutboxMessage;
import com.library.borrow.domain.OutboxRepository;
import com.library.common.api.ApiException;
import com.library.common.event.EventType;
import com.library.common.event.BorrowEvent;
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
public class BorrowStore {

    private static final Logger log = LoggerFactory.getLogger(BorrowStore.class);
    private static final List<BorrowStatus> OPEN_STATUSES = List.of(BorrowStatus.PENDING, BorrowStatus.ACTIVE, BorrowStatus.OVERDUE);

    private final BorrowRepository borrows;
    private final BorrowEventRepository events;
    private final OutboxRepository outbox;
    private final IdempotencyRepository idempotency;
    private final BorrowProperties properties;
    private final ObjectMapper objectMapper;

    public BorrowStore(BorrowRepository borrows, BorrowEventRepository events, OutboxRepository outbox,
                     IdempotencyRepository idempotency, BorrowProperties properties,
                     ObjectMapper objectMapper) {
        this.borrows = borrows;
        this.events = events;
        this.outbox = outbox;
        this.idempotency = idempotency;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BorrowRecord createPending(UUID userId, UUID bookId, String actor) {
        BorrowRecord borrow = borrows.save(new BorrowRecord(userId, bookId));
        appendEvent(borrow, "REQUESTED", actor, null);
        return borrow;
    }

    @Transactional
    public BorrowRecord activate(UUID borrowId, String bookTitle, String actor) {
        BorrowRecord borrow = require(borrowId);
        borrow.activate(bookTitle, properties.getBorrowRecordPeriodDays());
        appendEvent(borrow, "ISSUED", actor, null);
        publish(EventType.BORROW_ISSUED, borrow, null);
        log.info("Книга выдана borrowId={} userId={} срок={}", borrowId, borrow.getUserId(), borrow.getDueDate());
        return borrow;
    }

    @Transactional
    public BorrowRecord reject(UUID borrowId, String reason, String actor) {
        BorrowRecord borrow = require(borrowId);
        if (borrow.getStatus() != BorrowStatus.PENDING) {
            log.warn("Откат саги borrowId={} невозможен: статус уже {}", borrowId, borrow.getStatus());
            return borrow;
        }
        borrow.reject(reason);
        appendEvent(borrow, "REJECTED", actor, reason);
        log.warn("Выдача отклонена borrowId={} причина={}", borrowId, reason);
        return borrow;
    }

    @Transactional
    public BorrowRecord markReturned(UUID borrowId, String actor) {
        BorrowRecord borrow = require(borrowId);
        borrow.markReturned();
        appendEvent(borrow, "RETURNED", actor, null);
        publish(EventType.BORROW_RETURNED, borrow, null);
        log.info("Книга возвращена borrowId={}", borrowId);
        return borrow;
    }

    @Transactional
    public BorrowRecord extend(UUID borrowId, String actor) {
        BorrowRecord borrow = require(borrowId);
        borrow.extend(properties.getBorrowRecordPeriodDays(), properties.getMaxExtensions());
        appendEvent(borrow, "EXTENDED", actor, null);
        log.info("Выдача продлена borrowId={} новый срок={}", borrowId, borrow.getDueDate());
        return borrow;
    }

    @Transactional
    public void markCopyReleased(UUID borrowId) {
        borrows.findById(borrowId).ifPresent(BorrowRecord::markCopyReleased);
    }

    @Transactional
    public void markCopyReleasePending(UUID borrowId) {
        borrows.findById(borrowId).ifPresent(BorrowRecord::markCopyReleasePending);
    }

    /** Помечает просроченные выдачи и складывает события в outbox. Возвращает число затронутых. */
    @Transactional
    public int flagOverdue() {
        List<BorrowRecord> overdue = borrows.findByStatusInAndDueDateBeforeAndOverdueNotifiedFalse(
                List.of(BorrowStatus.ACTIVE, BorrowStatus.OVERDUE), LocalDate.now());
        for (BorrowRecord borrow : overdue) {
            borrow.markOverdue();
            appendEvent(borrow, "OVERDUE", "system", null);
            publish(EventType.BORROW_OVERDUE, borrow, borrow.daysOverdue(LocalDate.now()));
        }
        return overdue.size();
    }

    /** Напоминания «срок подходит»: событие уходит один раз на выдачу (после продления — снова). */
    @Transactional
    public int remindDueSoon() {
        LocalDate threshold = LocalDate.now().plusDays(properties.getDueSoonDays());
        List<BorrowRecord> dueSoon = borrows.findByStatusAndDueDateLessThanEqualAndDueSoonNotifiedFalse(
                BorrowStatus.ACTIVE, threshold);
        for (BorrowRecord borrow : dueSoon) {
            borrow.markDueSoonNotified();
            publish(EventType.BORROW_DUE_SOON, borrow, null);
        }
        return dueSoon.size();
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> stuckSagas() {
        return borrows.findByStatusAndRequestedAtBefore(BorrowStatus.PENDING,
                Instant.now().minus(properties.getSagaTimeout()));
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> pendingCopyReleases() {
        return borrows.findByCopyReleasePendingTrue();
    }

    @Transactional(readOnly = true)
    public BorrowRecord require(UUID borrowId) {
        return borrows.findById(borrowId)
                .orElseThrow(() -> ApiException.notFound("borrow_not_found", "Выдача не найдена: " + borrowId));
    }

    @Transactional(readOnly = true)
    public long countOpenBorrows(UUID userId) {
        return borrows.countByUserIdAndStatusIn(userId, OPEN_STATUSES);
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecord> byUser(UUID userId, Pageable pageable) {
        return borrows.findByUserIdOrderByRequestedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecord> byStatus(BorrowStatus status, Pageable pageable) {
        return status == null ? borrows.findAll(pageable) : borrows.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<BorrowEventRecord> history(UUID borrowId) {
        require(borrowId);
        return events.findByBorrowIdOrderBySeqAsc(borrowId);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> borrowIdByIdempotencyKey(String key) {
        return idempotency.findById(key).map(IdempotencyRecord::getBorrowId);
    }

    @Transactional
    public void rememberIdempotencyKey(String key, UUID borrowId) {
        idempotency.save(new IdempotencyRecord(key, borrowId));
    }

    /** Дописывает событие в журнал. UPDATE по журналу не делается никогда. */
    private void appendEvent(BorrowRecord borrow, String type, String actor, String reason) {
        int seq = events.countByBorrowId(borrow.getId()) + 1;
        events.save(new BorrowEventRecord(borrow.getId(), seq, type, snapshot(borrow, reason), actor));
    }

    /** Кладёт событие в outbox — в ту же транзакцию, что и изменение выдачи. */
    private void publish(String type, BorrowRecord borrow, Integer daysOverdue) {
        BorrowEvent event = BorrowEvent.of(type, borrow.getId(), borrow.getUserId(), borrow.getBookId(),
                borrow.getBookTitle(), borrow.getDueDate(), daysOverdue);
        outbox.save(new OutboxMessage(event.eventId(), type, borrow.getId(), toJson(event)));
    }

    private String snapshot(BorrowRecord borrow, String reason) {
        return toJson(new BorrowRecordSnapshot(borrow.getId(), borrow.getUserId(), borrow.getBookId(), borrow.getBookTitle(),
                borrow.getStatus().name(), borrow.getDueDate(), borrow.getExtensions(), reason));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось сериализовать событие", ex);
        }
    }

    private record BorrowRecordSnapshot(UUID borrowId, UUID userId, UUID bookId, String bookTitle, String status,
                                LocalDate dueDate, int extensions, String reason) {
    }
}
