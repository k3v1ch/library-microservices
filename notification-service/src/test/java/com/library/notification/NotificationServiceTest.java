package com.library.notification;

import com.library.common.event.EventType;
import com.library.common.event.LoanEvent;
import com.library.notification.client.ReadersClient;
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
    private ReadersClient readersClient;

    @Test
    void sameEventDeliveredTwiceCreatesOneNotification() {
        UUID readerId = UUID.randomUUID();
        when(readersClient.contact(readerId))
                .thenReturn(new ReadersClient.ReaderContact(readerId, "Иван Петров", "ivan@example.com", null));
        LoanEvent event = LoanEvent.of(EventType.LOAN_ISSUED, UUID.randomUUID(), readerId, UUID.randomUUID(),
                "Чистый код", LocalDate.now().plusDays(14), null);

        notificationService.handle(event);
        notificationService.handle(event);

        List<Notification> saved = notifications.findAll().stream()
                .filter(notification -> notification.getReaderId().equals(readerId))
                .toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStatus()).isEqualTo(Notification.Status.SENT);
        assertThat(saved.getFirst().getSubject()).isEqualTo("Книга выдана");
        assertThat(saved.getFirst().getBody()).contains("Чистый код").contains("Вернуть до");
    }

    @Test
    void overdueEventProducesOverdueText() {
        UUID readerId = UUID.randomUUID();
        when(readersClient.contact(readerId))
                .thenReturn(new ReadersClient.ReaderContact(readerId, "Анна", "anna@example.com", null));

        notificationService.handle(LoanEvent.of(EventType.LOAN_OVERDUE, UUID.randomUUID(), readerId,
                UUID.randomUUID(), "Рефакторинг", LocalDate.now().minusDays(3), 3));

        Notification notification = notifications.findAll().stream()
                .filter(candidate -> candidate.getReaderId().equals(readerId))
                .findFirst().orElseThrow();
        assertThat(notification.getSubject()).isEqualTo("Книга просрочена");
        assertThat(notification.getBody()).contains("просрочена на 3 дн");
    }

    @Test
    void notificationIsStoredAsFailedWhenContactsAreUnavailable() {
        UUID readerId = UUID.randomUUID();
        when(readersClient.contact(any())).thenReturn(null);

        notificationService.handle(LoanEvent.of(EventType.LOAN_ISSUED, UUID.randomUUID(), readerId,
                UUID.randomUUID(), "Книга", LocalDate.now().plusDays(7), null));

        Notification notification = notifications.findAll().stream()
                .filter(candidate -> candidate.getReaderId().equals(readerId))
                .findFirst().orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(Notification.Status.FAILED);
        assertThat(notification.getError()).contains("контакты");
    }
}
