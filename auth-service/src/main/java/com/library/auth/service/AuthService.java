package com.library.auth.service;

import com.library.auth.config.AuthProperties;
import com.library.auth.domain.UserAccount;
import com.library.auth.domain.UserAccountRepository;
import com.library.common.api.ApiException;
import com.library.common.client.TokenResponse;
import com.library.common.security.SecurityDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final AuthProperties properties;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder,
                       JwtIssuer jwtIssuer, AuthProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
        this.properties = properties;
    }

    @Transactional
    public UserAccount register(String username, String rawPassword, List<String> roles) {
        if (users.existsByUsername(username)) {
            throw ApiException.conflict("username_taken", "Логин уже занят");
        }
        UserAccount user = new UserAccount(username, passwordEncoder.encode(rawPassword),
                roles.isEmpty() ? List.of(SecurityDefaults.ROLE_READER) : roles);
        users.save(user);
        log.info("Зарегистрирован пользователь username={} roles={}", username, user.getRoles());
        return user;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String username, String rawPassword) {
        UserAccount user = users.findByUsername(username)
                .filter(candidate -> passwordEncoder.matches(rawPassword, candidate.getPasswordHash()))
                // Одинаковый ответ на «нет логина» и «неверный пароль»: не подсказываем, что логин существует.
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                        "Неверный логин или пароль"));
        log.info("Вход пользователя username={}", username);
        return jwtIssuer.issueUserToken(user);
    }

    public TokenResponse issueServiceToken(String grantType, String clientId, String clientSecret) {
        if (!"client_credentials".equals(grantType)) {
            throw ApiException.badRequest("unsupported_grant_type", "Поддерживается только client_credentials");
        }
        String expected = properties.getClients().get(clientId);
        if (expected == null || !constantTimeEquals(expected, clientSecret)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_client", "Неверные данные клиента");
        }
        return jwtIssuer.issueServiceToken(clientId);
    }

    /** Сравнение секретов за постоянное время: защита от подбора по времени ответа. */
    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
