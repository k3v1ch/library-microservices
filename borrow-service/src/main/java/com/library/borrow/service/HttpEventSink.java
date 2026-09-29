package com.library.borrow.service;

import com.library.common.event.BorrowEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Доставка событий по HTTP — режим без брокера (профиль по умолчанию). */
@Component
@Profile("!kafka")
public class HttpEventSink implements EventSink {

    private final RestClient notifications;

    public HttpEventSink(@Qualifier("notificationsRestClient") RestClient notifications) {
        this.notifications = notifications;
    }

    @Override
    public void send(BorrowEvent event) {
        notifications.post()
                .uri("/internal/events")
                .header("Idempotency-Key", event.eventId().toString())
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public String name() {
        return "http:notification-service";
    }
}
