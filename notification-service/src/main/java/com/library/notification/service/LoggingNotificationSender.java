package com.library.notification.service;

import com.library.common.util.Pii;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Отправка «письма» в лог: видно в консоли сервиса, не требует SMTP для демонстрации. */
@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String recipient, String subject, String body) {
        log.info("=== ПИСЬМО ===\nКому: {}\nТема: {}\n{}\n==============",
                Pii.maskEmail(recipient), subject, body);
    }
}
