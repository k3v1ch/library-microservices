package com.library.user;

import com.library.common.api.ApiException;
import com.library.user.domain.User;
import com.library.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void registersUserWithCardNumberAndActiveStatus() {
        User reader = userService.register(UUID.randomUUID(), "Иван Петров", "ivan@example.com", "+7 999 000-11-22");

        assertThat(reader.getCardNumber()).startsWith("LIB-");
        assertThat(reader.getStatus()).isEqualTo(User.Status.ACTIVE);
        assertThat(userService.requireByUserId(reader.getUserId()).getId()).isEqualTo(reader.getId());
    }

    @Test
    void oneAccountCannotHaveTwoProfiles() {
        UUID userId = UUID.randomUUID();
        userService.register(userId, "Анна Смирнова", "anna@example.com", null);

        assertThatThrownBy(() -> userService.register(userId, "Анна С.", "anna2@example.com", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Профиль читателя уже создан");
    }

    @Test
    void blockedUserCannotBorrow() {
        User reader = userService.register(UUID.randomUUID(), "Пётр Сидоров", "petr@example.com", null);

        User blocked = userService.block(reader.getId(), "Задолженность больше 30 дней");
        assertThat(blocked.getStatus()).isEqualTo(User.Status.BLOCKED);
        assertThat(blocked.getBlockedReason()).contains("Задолженность");

        assertThat(userService.unblock(reader.getId()).getStatus()).isEqualTo(User.Status.ACTIVE);
    }
}
