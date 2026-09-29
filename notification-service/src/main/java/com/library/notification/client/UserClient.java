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
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient rest;

    public UserClient(InternalRestClientFactory factory,
                         @Value("${notification.user-url}") String usersUrl) {
        this.rest = factory.forBaseUrl(usersUrl);
    }

    public UserContact contact(UUID userId) {
        try {
            return rest.get()
                    .uri("/internal/users/{id}/contact", userId)
                    .retrieve()
                    .body(UserContact.class);
        } catch (RuntimeException ex) {
            log.error("Не удалось получить контакты userId={}: {}", userId, ex.toString());
            return null;
        }
    }

    public record UserContact(UUID userId, String fullName, String email, String phone) {
    }
}
