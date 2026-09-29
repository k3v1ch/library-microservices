package com.library.borrow.service;

import com.library.borrow.client.BookClient;
import com.library.borrow.domain.BorrowRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Фоновые задачи: просрочки, зависшие саги, неосвобождённые экземпляры. */
@Component
@ConditionalOnProperty(name = "borrow.scheduling-enabled", matchIfMissing = true)
public class BorrowMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(BorrowMaintenanceScheduler.class);

    private final BorrowStore store;
    private final BookClient bookClient;

    public BorrowMaintenanceScheduler(BorrowStore store, BookClient bookClient) {
        this.store = store;
        this.bookClient = bookClient;
    }

    @Scheduled(fixedDelayString = "${borrow.schedule.overdue-interval-ms:60000}")
    public void checkOverdue() {
        int overdue = store.flagOverdue();
        int dueSoon = store.remindDueSoon();
        if (overdue > 0 || dueSoon > 0) {
            log.info("Проверка сроков: просрочено={} напоминаний={}", overdue, dueSoon);
        }
    }

    @Scheduled(fixedDelayString = "${borrow.schedule.saga-interval-ms:30000}")
    public void rollbackStuckSagas() {
        List<BorrowRecord> stuck = store.stuckSagas();
        for (BorrowRecord borrow : stuck) {
            log.warn("Сага зависла borrowId={} с {} — откатываю", borrow.getId(), borrow.getRequestedAt());
            store.reject(borrow.getId(), "Сага не завершилась за отведённое время", "system");
            releaseQuietly(borrow);
        }
    }

    @Scheduled(fixedDelayString = "${borrow.schedule.release-interval-ms:30000}")
    public void retryPendingReleases() {
        for (BorrowRecord borrow : store.pendingCopyReleases()) {
            releaseQuietly(borrow);
        }
    }

    private void releaseQuietly(BorrowRecord borrow) {
        try {
            bookClient.releaseCopy(borrow.getId());
            store.markCopyReleased(borrow.getId());
            log.info("Экземпляр освобождён повторной попыткой borrowId={}", borrow.getId());
        } catch (RuntimeException ex) {
            store.markCopyReleasePending(borrow.getId());
            log.warn("Экземпляр всё ещё не освобождён borrowId={}: {}", borrow.getId(), ex.toString());
        }
    }
}
