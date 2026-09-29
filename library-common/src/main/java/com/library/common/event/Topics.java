package com.library.common.event;

/** Имена топиков Kafka. В имени зашита версия схемы: ломающее изменение = новый топик. */
public final class Topics {

    public static final String LOANS = "library.loans.v1";

    private Topics() {
    }
}
