package com.library.auth.config;

import com.library.auth.domain.UserAccountRepository;
import com.library.auth.service.AuthService;
import com.library.common.security.SecurityDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Создаёт учётную запись библиотекаря при первом запуске. */
@Configuration
@ConditionalOnProperty(name = "auth.bootstrap.enabled", matchIfMissing = true)
public class BootstrapUsers {

    private static final Logger log = LoggerFactory.getLogger(BootstrapUsers.class);

    @Bean
    public ApplicationRunner createLibrarian(AuthService authService, UserAccountRepository users,
                                             AuthProperties properties) {
        return args -> {
            String username = properties.getBootstrap().getLibrarianUsername();
            if (users.existsByUsername(username)) {
                return;
            }
            authService.register(username, properties.getBootstrap().getLibrarianPassword(),
                    List.of(SecurityDefaults.ROLE_LIBRARIAN));
            log.warn("Создан библиотекарь по умолчанию '{}' — смените пароль перед реальным использованием",
                    username);
        };
    }
}
