package com.library.common.client;

import com.library.common.api.ApiException;
import com.library.common.config.LibraryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/** Токен для вызовов «сервис → сервис»: grant_type=client_credentials, scope=internal. */
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);

    private final RestClient authClient;
    private final LibraryProperties.Auth props;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public ServiceTokenProvider(RestClient authClient, LibraryProperties.Auth props) {
        this.authClient = authClient;
        this.props = props;
    }

    public String token() {
        CachedToken cached = cache.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        synchronized (this) {
            CachedToken current = cache.get();
            if (current != null && current.isValid()) {
                return current.token();
            }
            CachedToken fresh = fetch();
            cache.set(fresh);
            return fresh.token();
        }
    }

    private CachedToken fetch() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        try {
            TokenResponse response = authClient.post()
                    .uri("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null) {
                throw ApiException.unavailable("auth_unavailable", "Сервис аутентификации вернул пустой ответ");
            }
            log.debug("Получен сервисный токен, срок жизни {} с", response.expiresIn());
            return new CachedToken(response.accessToken(),
                    Instant.now().plusSeconds(response.expiresIn()).minus(EXPIRY_MARGIN));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Не удалось получить сервисный токен: {}", ex.getMessage());
            throw ApiException.unavailable("auth_unavailable", "Сервис аутентификации недоступен");
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
