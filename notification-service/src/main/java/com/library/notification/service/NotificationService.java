package com.library.notification.service;

import com.library.common.event.EventType;
import com.library.common.event.LoanEvent;
import com.library.common.util.Pii;
import com.library.notification.client.ReadersClient;
import com.library.notification.domain.Notification;
import com.library.notification.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Обработка события выдачи: собрать текст, узнать контакт читателя, отправить. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String CHANNEL_EMAIL = "EMAIL";

    private final NotificationRepository notifications;
    private final ReadersClient readersClient;
    private final NotificationSender sender;

    public NotificationService(NotificationRepository notifications, ReadersClient readersClient,
                               NotificationSender sender) {
        this.notifications = notifications;
        this.readersClient = readersClient;
        this.sender = sender;
    }

    @Transactional
    public void handle(LoanEvent event) {
        if (notifications.existsByEventId(event.eventId())) {
            log.info("Событие {} уже обработано — повтор игнорирую", event.eventId());
            return;
        }

        String subject = subjectFor(event);
        ReadersClient.ReaderContact contact = readersClient.contact(event.readerId());
        String body = bodyFor(event, contact == null ? "читатель" : contact.fullName());
        String recipient = contact == null ? null : contact.email();

        Notification notification = new Notification(event.eventId(), event.type(), event.readerId(),
                CHANNEL_EMAIL, recipient, subject, body);

        if (recipient == null) {
            notification.markFailed("Не удалось получить контакты читателя");
            log.error("Уведомление не отправлено: нет контактов readerId={}", event.readerId());
        } else {
            try {
                sender.send(recipient, subject, body);
                log.info("Уведомление отправлено тип={} readerId={} адрес={}",
                        event.type(), event.readerId(), Pii.maskEmail(recipient));
            } catch (RuntimeException ex) {
                notification.markFailed(ex.getMessage());
                log.error("Ошибка отправки уведомления readerId={}: {}", event.readerId(), ex.toString());
            }
        }
        notifications.save(notification);
    }

    private String subjectFor(LoanEvent event) {
        return switch (event.type()) {
            case EventType.LOAN_ISSUED -> "Книга выдана";
            case EventType.LOAN_DUE_SOON -> "Срок возврата подходит";
            case EventType.LOAN_OVERDUE -> "Книга просрочена";
            case EventType.LOAN_RETURNED -> "Книга принята";
            default -> "Библиотека: уведомление";
        };
    }

    private String bodyFor(LoanEvent event, String name) {
        String title = event.bookTitle() == null ? "книга" : "«" + event.bookTitle() + "»";
        return switch (event.type()) {
            case EventType.LOAN_ISSUED -> "%s, %s выдана. Вернуть до %s.".formatted(name, title, event.dueDate());
            case EventType.LOAN_DUE_SOON -> "%s, срок возврата %s — %s. Можно продлить в личном кабинете."
                    .formatted(name, title, event.dueDate());
            case EventType.LOAN_OVERDUE -> "%s, %s просрочена на %d дн. Пожалуйста, верните книгу."
                    .formatted(name, title, event.daysOverdue() == null ? 0 : event.daysOverdue());
            case EventType.LOAN_RETURNED -> "%s, возврат %s принят. Спасибо!".formatted(name, title);
            default -> "%s, по вашей выдаче произошло событие %s.".formatted(name, event.type());
        };
    }
}
