package com.library.borrow.client;

import com.library.common.api.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/** Клиент сервиса читателей: только статус, без персональных данных. */
@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient rest;

    public UserClient(@Qualifier("userRestClient") RestClient rest) {
        this.rest = rest;
    }

    @Retry(name = "users")
    @CircuitBreaker(name = "users", fallbackMethod = "statusFallback")
    public UserStatusView status(UUID userId) {
        return rest.get()
                .uri("/internal/users/{id}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("users", response);
                })
                .body(UserStatusView.class);
    }

    @Retry(name = "users")
    @CircuitBreaker(name = "users", fallbackMethod = "statusFallback")
    public UserStatusView byUserId(UUID userId) {
        return rest.get()
                .uri("/internal/users/by-user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("users", response);
                })
                .body(UserStatusView.class);
    }

    private UserStatusView statusFallback(UUID id, ApiException ex) {
        throw ex;
    }

    private UserStatusView statusFallback(UUID id, Throwable ex) {
        log.error("Сервис читателей недоступен id={}: {}", id, ex.toString());
        throw ApiException.unavailable("user_service_unavailable", "Сервис читателей недоступен, попробуйте позже");
    }

    public record UserStatusView(UUID userId, String status, boolean canBorrow) {
    }
}
