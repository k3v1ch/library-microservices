package com.library.borrow.client;

import org.springframework.http.HttpStatus;

/** Техническая ошибка соседнего сервиса (5xx). */
public class RemoteServiceException extends RuntimeException {

    private final String service;
    private final HttpStatus status;

    public RemoteServiceException(String service, HttpStatus status, String message) {
        super("%s: %s (%s)".formatted(service, message, status));
        this.service = service;
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
