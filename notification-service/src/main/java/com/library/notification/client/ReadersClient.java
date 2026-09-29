package com.library.notification.client;

import com.library.common.client.InternalRestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/** Контакты читателя. */
@Component
public class ReadersClient {

    private static final Logger log = LoggerFactory.getLogger(ReadersClient.class);

    private final RestClient rest;

    public ReadersClient(InternalRestClientFactory factory,
                         @Value("${notification.readers-url}") String readersUrl) {
        this.rest = factory.forBaseUrl(readersUrl);
    }

    public ReaderContact contact(UUID readerId) {
        try {
            return rest.get()
                    .uri("/internal/v1/readers/{id}/contact", readerId)
                    .retrieve()
                    .body(ReaderContact.class);
        } catch (RuntimeException ex) {
            log.error("Не удалось получить контакты readerId={}: {}", readerId, ex.toString());
            return null;
        }
    }

    public record ReaderContact(UUID readerId, String fullName, String email, String phone) {
    }
}
