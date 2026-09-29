package com.library.circulation.service;

import com.library.circulation.config.CirculationProperties;
import com.library.circulation.domain.OutboxMessage;
import com.library.circulation.domain.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Доступ к outbox короткими транзакциями: упавшее сообщение не откатывает остальные. */
@Service
public class OutboxStore {

    private final OutboxRepository outbox;
    private final CirculationProperties properties;

    public OutboxStore(OutboxRepository outbox, CirculationProperties properties) {
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
