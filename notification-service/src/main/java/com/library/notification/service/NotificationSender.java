package com.library.notification.service;

/** Канал доставки. */
public interface NotificationSender {

    void send(String recipient, String subject, String body);
}
