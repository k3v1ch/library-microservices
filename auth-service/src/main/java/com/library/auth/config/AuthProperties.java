package com.library.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Настройки выпуска токенов. Секреты клиентов в проде приходят из Vault, а не из yml. */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String issuer = "http://localhost:8081";
    private Duration tokenTtl = Duration.ofMinutes(15);
    private Duration serviceTokenTtl = Duration.ofMinutes(5);
    /** clientId → clientSecret для grant_type=client_credentials. */
    private Map<String, String> clients = new LinkedHashMap<>();
    private final Bootstrap bootstrap = new Bootstrap();

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    /** Учётная запись первого библиотекаря: роль LIBRARIAN через публичный API не выдаётся. */
    public static class Bootstrap {
        private String librarianUsername = "librarian";
        private String librarianPassword = "librarian-pass";

        public String getLibrarianUsername() {
            return librarianUsername;
        }

        public void setLibrarianUsername(String librarianUsername) {
            this.librarianUsername = librarianUsername;
        }

        public String getLibrarianPassword() {
            return librarianPassword;
        }

        public void setLibrarianPassword(String librarianPassword) {
            this.librarianPassword = librarianPassword;
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public Duration getServiceTokenTtl() {
        return serviceTokenTtl;
    }

    public void setServiceTokenTtl(Duration serviceTokenTtl) {
        this.serviceTokenTtl = serviceTokenTtl;
    }

    public Map<String, String> getClients() {
        return clients;
    }

    public void setClients(Map<String, String> clients) {
        this.clients = clients;
    }
}
