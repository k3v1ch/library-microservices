package com.library.borrow.service;

import com.library.borrow.config.BorrowProperties;
import com.library.borrow.domain.OutboxMessage;
import com.library.borrow.domain.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Доступ к outbox короткими транзакциями: упавшее сообщение не откатывает остальные. */
@Service
public class OutboxStore {

    private final OutboxRepository outbox;
    private final BorrowProperties properties;

    public OutboxStore(OutboxRepository outbox, BorrowProperties properties) {
        this.outbox = outbox;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<OutboxMessage> nextBatch() {
        return outbox.findTop50ByPublishedAtIsNullAndAttemptsLessThanOrderByIdAsc(properties.getOutboxMaxAttempts());
    }

    @Transactional
    public void markPublished(Long id) {
        outbox.findById(id).ifPresent(OutboxMessage::markPublished);
    }

    @Transactional
    public void markFailed(Long id, String error) {
        outbox.findById(id).ifPresent(message -> message.markFailed(error));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return outbox.countByPublishedAtIsNull();
    }
}
