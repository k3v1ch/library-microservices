package com.library.notification.service;

import com.library.common.event.BorrowEvent;
import com.library.common.event.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Приём событий из Kafka (профиль kafka, используется в docker-compose). */
@Component
@Profile("kafka")
public class BorrowEventListener {

    private static final Logger log = LoggerFactory.getLogger(BorrowEventListener.class);

    private final NotificationService notificationService;

    public BorrowEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = Topics.BORROW, groupId = "notification-service")
    public void onBorrowEvent(BorrowEvent event) {
        log.debug("Получено событие {} из Kafka", event.type());
        notificationService.handle(event);
    }
}
