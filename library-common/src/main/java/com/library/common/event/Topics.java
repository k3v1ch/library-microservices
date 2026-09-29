package com.library.common.event;

/** Имена топиков Kafka. В имени зашита версия схемы: ломающее изменение = новый топик. */
public final class Topics {

    public static final String BORROW = "library.borrow.v1";

    private Topics() {
    }
}
