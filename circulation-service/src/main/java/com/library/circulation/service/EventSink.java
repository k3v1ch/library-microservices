package com.library.circulation.service;

import com.library.common.event.LoanEvent;

/** Куда relay отправляет события из outbox. */
public interface EventSink {

    void send(LoanEvent event);

    String name();
}
