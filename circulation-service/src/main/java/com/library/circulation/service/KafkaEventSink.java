package com.library.circulation.service;

import com.library.common.event.LoanEvent;
import com.library.common.event.Topics;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Продакшен-транспорт: Kafka. */
@Component
@Profile("kafka")
public class KafkaEventSink implements EventSink {

    private final KafkaTemplate<String, LoanEvent> kafkaTemplate;

    public KafkaEventSink(KafkaTemplate<String, LoanEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(LoanEvent event) {
        kafkaTemplate.send(Topics.LOANS, event.loanId().toString(), event).join();
    }

    @Override
    public String name() {
        return "kafka:" + Topics.LOANS;
    }
}
