package com.library.circulation.service;

import com.library.circulation.client.CatalogClient;
import com.library.circulation.domain.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Фоновые задачи: просрочки, зависшие саги, неосвобождённые экземпляры. */
@Component
@ConditionalOnProperty(name = "circulation.scheduling-enabled", matchIfMissing = true)
public class LoanMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanMaintenanceScheduler.class);

    private final LoanStore store;
    private final CatalogClient catalogClient;

    public LoanMaintenanceScheduler(LoanStore store, CatalogClient catalogClient) {
        this.store = store;
        this.catalogClient = catalogClient;
    }

    @Scheduled(fixedDelayString = "${circulation.schedule.overdue-interval-ms:60000}")
    public void checkOverdue() {
        int overdue = store.flagOverdue();
        int dueSoon = store.remindDueSoon();
        if (overdue > 0 || dueSoon > 0) {
            log.info("Проверка сроков: просрочено={} напоминаний={}", overdue, dueSoon);
        }
    }

    @Scheduled(fixedDelayString = "${circulation.schedule.saga-interval-ms:30000}")
    public void rollbackStuckSagas() {
        List<Loan> stuck = store.stuckSagas();
        for (Loan loan : stuck) {
            log.warn("Сага зависла loanId={} с {} — откатываю", loan.getId(), loan.getRequestedAt());
            store.reject(loan.getId(), "Сага не завершилась за отведённое время", "system");
            releaseQuietly(loan);
        }
    }

    @Scheduled(fixedDelayString = "${circulation.schedule.release-interval-ms:30000}")
    public void retryPendingReleases() {
        for (Loan loan : store.pendingCopyReleases()) {
            releaseQuietly(loan);
        }
    }

    private void releaseQuietly(Loan loan) {
        try {
            catalogClient.releaseCopy(loan.getId());
            store.markCopyReleased(loan.getId());
            log.info("Экземпляр освобождён повторной попыткой loanId={}", loan.getId());
        } catch (RuntimeException ex) {
            store.markCopyReleasePending(loan.getId());
            log.warn("Экземпляр всё ещё не освобождён loanId={}: {}", loan.getId(), ex.toString());
        }
    }
}
