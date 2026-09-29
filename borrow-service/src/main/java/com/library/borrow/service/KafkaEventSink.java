package com.library.borrow.service;

import com.library.common.event.BorrowEvent;
import com.library.common.event.Topics;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Продакшен-транспорт: Kafka. */
@Component
@Profile("kafka")
public class KafkaEventSink implements EventSink {

    private final KafkaTemplate<String, BorrowEvent> kafkaTemplate;

    public KafkaEventSink(KafkaTemplate<String, BorrowEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(BorrowEvent event) {
        kafkaTemplate.send(Topics.BORROW, event.borrowId().toString(), event).join();
    }

    @Override
    public String name() {
        return "kafka:" + Topics.BORROW;
    }
}
