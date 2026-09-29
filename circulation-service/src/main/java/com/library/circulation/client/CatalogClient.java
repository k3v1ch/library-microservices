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

/** Клиент каталога. */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final RestClient rest;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient rest) {
        this.rest = rest;
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog", fallbackMethod = "reserveFallback")
    public ReservationView reserveCopy(UUID loanId, UUID bookId) {
        return rest.post()
                .uri("/internal/v1/reservations")
                .body(new ReserveCommand(loanId, bookId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("catalog", response);
                })
                .body(ReservationView.class);
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog", fallbackMethod = "releaseFallback")
    public void releaseCopy(UUID loanId) {
        rest.delete()
                .uri("/internal/v1/reservations/{loanId}", loanId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw ErrorTranslator.translate("catalog", response);
                })
                .toBodilessEntity();
    }

    /** Бизнес-ошибку каталога отдаём саге как есть: она решит, отклонять выдачу или нет. */
    private ReservationView reserveFallback(UUID loanId, UUID bookId, ApiException ex) {
        throw ex;
    }

    private ReservationView reserveFallback(UUID loanId, UUID bookId, Throwable ex) {
        log.error("Каталог недоступен при резервировании loanId={}: {}", loanId, ex.toString());
        throw ApiException.unavailable("catalog_unavailable",
                "Каталог недоступен, попробуйте позже");
    }

    private void releaseFallback(UUID loanId, ApiException ex) {
        throw ex;
    }

    private void releaseFallback(UUID loanId, Throwable ex) {
        log.error("Каталог недоступен при освобождении экземпляра loanId={}: {}", loanId, ex.toString());
        throw ApiException.unavailable("catalog_unavailable", "Каталог недоступен");
    }

    public record ReserveCommand(UUID loanId, UUID bookId) {
    }

    public record ReservationView(UUID loanId, UUID bookId, String status, String bookTitle) {
    }
}
