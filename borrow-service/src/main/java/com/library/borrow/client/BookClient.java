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

/** Клиент каталога. */
@Component
public class BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClient.class);

    private final RestClient rest;

    public BookClient(@Qualifier("bookRestClient") RestClient rest) {
        this.rest = rest;
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog", fallbackMethod = "reserveFallback")
    public ReservationView reserveCopy(UUID borrowId, UUID bookId) {
        return rest.post()
                .uri("/internal/reservations")
                .body(new ReserveCommand(borrowId, bookId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("catalog", response);
                })
                .body(ReservationView.class);
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog", fallbackMethod = "releaseFallback")
    public void releaseCopy(UUID borrowId) {
        rest.delete()
                .uri("/internal/reservations/{borrowId}", borrowId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("catalog", response);
                })
                .toBodilessEntity();
    }

    /** Бизнес-ошибку каталога отдаём саге как есть: она решит, отклонять выдачу или нет. */
    private ReservationView reserveFallback(UUID borrowId, UUID bookId, ApiException ex) {
        throw ex;
    }

    private ReservationView reserveFallback(UUID borrowId, UUID bookId, Throwable ex) {
        log.error("Каталог недоступен при резервировании borrowId={}: {}", borrowId, ex.toString());
        throw ApiException.unavailable("book_service_unavailable",
                "Каталог недоступен, попробуйте позже");
    }

    private void releaseFallback(UUID borrowId, ApiException ex) {
        throw ex;
    }

    private void releaseFallback(UUID borrowId, Throwable ex) {
        log.error("Каталог недоступен при освобождении экземпляра borrowId={}: {}", borrowId, ex.toString());
        throw ApiException.unavailable("book_service_unavailable", "Каталог недоступен");
    }

    public record ReserveCommand(UUID borrowId, UUID bookId) {
    }

    public record ReservationView(UUID borrowId, UUID bookId, String status, String bookTitle) {
    }
}
