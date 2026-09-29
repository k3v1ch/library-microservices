package com.library.common.event;

/** Типы доменных событий, которые публикует сервис выдачи. Часть публичного контракта. */
public final class EventType {

    public static final String BORROW_ISSUED = "borrow.issued";
    public static final String BORROW_RETURNED = "borrow.returned";
    public static final String BORROW_DUE_SOON = "borrow.due-soon";
    public static final String BORROW_OVERDUE = "borrow.overdue";

    private EventType() {
    }
}
