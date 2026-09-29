package com.library.circulation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.circulation.config.CirculationProperties;
import com.library.circulation.domain.OutboxMessage;
import com.library.common.event.LoanEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Relay: вынимает события из outbox и отправляет их в брокер (или в HTTP-получателя). */
@Component
@ConditionalOnProperty(name = "circulation.scheduling-enabled", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore store;
    private final EventSink sink;
    private final ObjectMapper objectMapper;
    private final CirculationProperties properties;
    private final Counter published;
    private final Counter failed;

    public OutboxRelay(OutboxStore store, EventSink sink, ObjectMapper objectMapper,
                       CirculationProperties properties, MeterRegistry meterRegistry) {
        this.store = store;
        this.sink = sink;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.published = Counter.builder("circulation.outbox.published")
                .description("Событий успешно отправлено из outbox").register(meterRegistry);
        this.failed = Counter.builder("circulation.outbox.failed")
                .description("Неудачных попыток отправки события").register(meterRegistry);
        Gauge.builder("circulation.outbox.pending", store, OutboxStore::pendingCount)
                .description("Событий ждёт отправки").register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${circulation.outbox.poll-interval-ms:2000}")
    public void publishPending() {
        List<OutboxMessage> batch = store.nextBatch();
        if (batch.isEmpty()) {
            return;
        }
        log.debug("Отправляю {} событий через {}", batch.size(), sink.name());
        for (OutboxMessage message : batch) {
            try {
                sink.send(objectMapper.readValue(message.getPayload(), LoanEvent.class));
                store.markPublished(message.getId());
                published.increment();
            } catch (Exception ex) {
                failed.increment();
                store.markFailed(message.getId(), ex.toString());
                int attempts = message.getAttempts() + 1;
                if (attempts >= properties.getOutboxMaxAttempts()) {
                    log.error("Событие {} ({}) не отправлено за {} попыток — требуется разбор вручную: {}",
                            message.getEventId(), message.getType(), attempts, ex.toString());
                } else {
                    log.warn("Событие {} не отправлено (попытка {}), повторю позже: {}",
                            message.getEventId(), attempts, ex.toString());
                }
            }
        }
    }
}
