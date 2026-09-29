package com.library.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Настройки, общие для всех сервисов: адрес сервиса аутентификации и таймауты вызовов. */
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    private final Auth auth = new Auth();
    private final Http http = new Http();

    public Auth getAuth() {
        return auth;
    }

    public Http getHttp() {
        return http;
    }

    public static class Auth {
        /** Базовый URL сервиса аутентификации; если не задан, сервисный токен не запрашивается. */
        private String baseUrl;
        private String clientId = "internal-service";
        private String clientSecret = "internal-secret";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(3);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
