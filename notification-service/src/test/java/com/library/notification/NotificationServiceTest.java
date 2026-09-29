package com.library.notification;

import com.library.common.event.EventType;
import com.library.common.event.BorrowEvent;
import com.library.notification.client.UserClient;
import com.library.notification.domain.Notification;
import com.library.notification.domain.NotificationRepository;
import com.library.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notifications;

    @MockitoBean
    private UserClient userClient;

    @Test
    void sameEventDeliveredTwiceCreatesOneNotification() {
        UUID userId = UUID.randomUUID();
        when(userClient.contact(userId))
                .thenReturn(new UserClient.UserContact(userId, "Иван Петров", "ivan@example.com", null));
        BorrowEvent event = BorrowEvent.of(EventType.BORROW_ISSUED, UUID.randomUUID(), userId, UUID.randomUUID(),
                "Чистый код", LocalDate.now().plusDays(14), null);

        notificationService.handle(event);
        notificationService.handle(event);

        List<Notification> saved = notifications.findAll().stream()
                .filter(notification -> notification.getUserId().equals(userId))
                .toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStatus()).isEqualTo(Notification.Status.SENT);
        assertThat(saved.getFirst().getSubject()).isEqualTo("Книга выдана");
        assertThat(saved.getFirst().getBody()).contains("Чистый код").contains("Вернуть до");
    }

    @Test
    void overdueEventProducesOverdueText() {
        UUID userId = UUID.randomUUID();
        when(userClient.contact(userId))
                .thenReturn(new UserClient.UserContact(userId, "Анна", "anna@example.com", null));

        notificationService.handle(BorrowEvent.of(EventType.BORROW_OVERDUE, UUID.randomUUID(), userId,
                UUID.randomUUID(), "Рефакторинг", LocalDate.now().minusDays(3), 3));

        Notification notification = notifications.findAll().stream()
                .filter(candidate -> candidate.getUserId().equals(userId))
                .findFirst().orElseThrow();
        assertThat(notification.getSubject()).isEqualTo("Книга просрочена");
        assertThat(notification.getBody()).contains("просрочена на 3 дн");
    }

    @Test
    void notificationIsStoredAsFailedWhenContactsAreUnavailable() {
        UUID userId = UUID.randomUUID();
        when(userClient.contact(any())).thenReturn(null);

        notificationService.handle(BorrowEvent.of(EventType.BORROW_ISSUED, UUID.randomUUID(), userId,
                UUID.randomUUID(), "Книга", LocalDate.now().plusDays(7), null));

        Notification notification = notifications.findAll().stream()
                .filter(candidate -> candidate.getUserId().equals(userId))
                .findFirst().orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(Notification.Status.FAILED);
        assertThat(notification.getError()).contains("контакты");
    }
}
