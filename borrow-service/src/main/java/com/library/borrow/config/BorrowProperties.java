package com.library.borrow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Правила выдачи и адреса соседних сервисов. */
@ConfigurationProperties(prefix = "borrow")
public class BorrowProperties {

    /** Срок выдачи книги в днях. */
    private int borrowPeriodDays = 14;
    /** Сколько книг читатель может держать одновременно. */
    private int maxActiveBorrows = 3;
    /** Сколько раз можно продлить одну выдачу. */
    private int maxExtensions = 2;
    /** За сколько дней до срока напоминать о возврате. */
    private int dueSoonDays = 2;
    /** Через сколько незавершённая сага считается зависшей и откатывается. */
    private Duration sagaTimeout = Duration.ofMinutes(2);
    /** Сколько раз relay пытается отправить событие, прежде чем отложить его как «мёртвое». */
    private int outboxMaxAttempts = 5;

    private final Services services = new Services();

    public int getBorrowRecordPeriodDays() {
        return borrowPeriodDays;
    }

    public void setBorrowRecordPeriodDays(int borrowPeriodDays) {
        this.borrowPeriodDays = borrowPeriodDays;
    }

    public int getMaxActiveBorrows() {
        return maxActiveBorrows;
    }

    public void setMaxActiveBorrows(int maxActiveBorrows) {
        this.maxActiveBorrows = maxActiveBorrows;
    }

    public int getMaxExtensions() {
        return maxExtensions;
    }

    public void setMaxExtensions(int maxExtensions) {
        this.maxExtensions = maxExtensions;
    }

    public int getDueSoonDays() {
        return dueSoonDays;
    }

    public void setDueSoonDays(int dueSoonDays) {
        this.dueSoonDays = dueSoonDays;
    }

    public Duration getSagaTimeout() {
        return sagaTimeout;
    }

    public void setSagaTimeout(Duration sagaTimeout) {
        this.sagaTimeout = sagaTimeout;
    }

    public int getOutboxMaxAttempts() {
        return outboxMaxAttempts;
    }

    public void setOutboxMaxAttempts(int outboxMaxAttempts) {
        this.outboxMaxAttempts = outboxMaxAttempts;
    }

    public Services getServices() {
        return services;
    }

    public static class Services {
        private String catalogUrl = "http://localhost:8082";
        private String usersUrl = "http://localhost:8083";
        private String notificationsUrl = "http://localhost:8085";

        public String getBookUrl() {
            return catalogUrl;
        }

        public void setBookUrl(String catalogUrl) {
            this.catalogUrl = catalogUrl;
        }

        public String getUserUrl() {
            return usersUrl;
        }

        public void setUserUrl(String usersUrl) {
            this.usersUrl = usersUrl;
        }

        public String getNotificationsUrl() {
            return notificationsUrl;
        }

        public void setNotificationsUrl(String notificationsUrl) {
            this.notificationsUrl = notificationsUrl;
        }
    }
}
