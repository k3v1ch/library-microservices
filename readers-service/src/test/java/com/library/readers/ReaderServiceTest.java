package com.library.readers;

import com.library.common.api.ApiException;
import com.library.readers.domain.Reader;
import com.library.readers.service.ReaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReaderServiceTest {

    @Autowired
    private ReaderService readerService;

    @Test
    void registersReaderWithCardNumberAndActiveStatus() {
        Reader reader = readerService.register(UUID.randomUUID(), "Иван Петров", "ivan@example.com", "+7 999 000-11-22");

        assertThat(reader.getCardNumber()).startsWith("LIB-");
        assertThat(reader.getStatus()).isEqualTo(Reader.Status.ACTIVE);
        assertThat(readerService.requireByUserId(reader.getUserId()).getId()).isEqualTo(reader.getId());
    }

    @Test
    void oneAccountCannotHaveTwoProfiles() {
        UUID userId = UUID.randomUUID();
        readerService.register(userId, "Анна Смирнова", "anna@example.com", null);

        assertThatThrownBy(() -> readerService.register(userId, "Анна С.", "anna2@example.com", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Профиль читателя уже создан");
    }

    @Test
    void blockedReaderCannotBorrow() {
        Reader reader = readerService.register(UUID.randomUUID(), "Пётр Сидоров", "petr@example.com", null);

        Reader blocked = readerService.block(reader.getId(), "Задолженность больше 30 дней");
        assertThat(blocked.getStatus()).isEqualTo(Reader.Status.BLOCKED);
        assertThat(blocked.getBlockedReason()).contains("Задолженность");

        assertThat(readerService.unblock(reader.getId()).getStatus()).isEqualTo(Reader.Status.ACTIVE);
    }
}
