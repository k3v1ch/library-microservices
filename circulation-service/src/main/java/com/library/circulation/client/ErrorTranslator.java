package com.library.circulation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Превращает ответ соседнего сервиса в доменную ошибку. */
final class ErrorTranslator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static ApiException translate(String service, ClientHttpResponse response) throws IOException {
        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String code = extract(body, "code", service + "_error");
        String detail = extract(body, "detail", "Сервис " + service + " вернул ошибку " + response.getStatusCode());

        if (status != null && status.is5xxServerError()) {
            // Сигнал для @Retry и @CircuitBreaker: ошибка техническая.
            throw new RemoteServiceException(service, status, detail);
        }
        return new ApiException(status == null ? HttpStatus.BAD_GATEWAY : status, code, detail);
    }

    private static String extract(String body, String field, String fallback) {
        try {
            JsonNode node = MAPPER.readTree(body).get(field);
            return node == null || node.isNull() ? fallback : node.asText();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private ErrorTranslator() {
    }
}
