package com.library.borrow.service;

import com.library.common.event.BorrowEvent;

/** Куда relay отправляет события из outbox. */
public interface EventSink {

    void send(BorrowEvent event);

    String name();
}
