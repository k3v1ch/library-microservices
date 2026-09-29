package com.library.circulation.client;

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
public class ReadersClient {

    private static final Logger log = LoggerFactory.getLogger(ReadersClient.class);

    private final RestClient rest;

    public ReadersClient(@Qualifier("readersRestClient") RestClient rest) {
        this.rest = rest;
    }

    @Retry(name = "readers")
    @CircuitBreaker(name = "readers", fallbackMethod = "statusFallback")
    public ReaderStatusView status(UUID readerId) {
        return rest.get()
                .uri("/internal/v1/readers/{id}", readerId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("readers", response);
                })
                .body(ReaderStatusView.class);
    }

    @Retry(name = "readers")
    @CircuitBreaker(name = "readers", fallbackMethod = "statusFallback")
    public ReaderStatusView byUserId(UUID userId) {
        return rest.get()
                .uri("/internal/v1/readers/by-user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("readers", response);
                })
                .body(ReaderStatusView.class);
    }

    private ReaderStatusView statusFallback(UUID id, ApiException ex) {
        throw ex;
    }

    private ReaderStatusView statusFallback(UUID id, Throwable ex) {
        log.error("Сервис читателей недоступен id={}: {}", id, ex.toString());
        throw ApiException.unavailable("readers_unavailable", "Сервис читателей недоступен, попробуйте позже");
    }

    public record ReaderStatusView(UUID readerId, String status, boolean canBorrow) {
    }
}
