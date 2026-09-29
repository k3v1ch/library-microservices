package com.library.common.event;

/** Типы доменных событий, которые публикует сервис выдачи. Часть публичного контракта. */
public final class EventType {

    public static final String LOAN_ISSUED = "loan.issued";
    public static final String LOAN_RETURNED = "loan.returned";
    public static final String LOAN_DUE_SOON = "loan.due-soon";
    public static final String LOAN_OVERDUE = "loan.overdue";

    private EventType() {
    }
}
