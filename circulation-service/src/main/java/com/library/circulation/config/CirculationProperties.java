package com.library.circulation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Правила выдачи и адреса соседних сервисов. */
@ConfigurationProperties(prefix = "circulation")
public class CirculationProperties {

    /** Срок выдачи книги в днях. */
    private int loanPeriodDays = 14;
    /** Сколько книг читатель может держать одновременно. */
    private int maxActiveLoans = 3;
    /** Сколько раз можно продлить одну выдачу. */
    private int maxExtensions = 2;
    /** За сколько дней до срока напоминать о возврате. */
    private int dueSoonDays = 2;
    /** Через сколько незавершённая сага считается зависшей и откатывается. */
    private Duration sagaTimeout = Duration.ofMinutes(2);
    /** Сколько раз relay пытается отправить событие, прежде чем отложить его как «мёртвое». */
    private int outboxMaxAttempts = 5;

    private final Services services = new Services();

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }

    public void setLoanPeriodDays(int loanPeriodDays) {
        this.loanPeriodDays = loanPeriodDays;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public void setMaxActiveLoans(int maxActiveLoans) {
        this.maxActiveLoans = maxActiveLoans;
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
        private String readersUrl = "http://localhost:8083";
        private String notificationsUrl = "http://localhost:8085";

        public String getCatalogUrl() {
            return catalogUrl;
        }

        public void setCatalogUrl(String catalogUrl) {
            this.catalogUrl = catalogUrl;
        }

        public String getReadersUrl() {
            return readersUrl;
        }

        public void setReadersUrl(String readersUrl) {
            this.readersUrl = readersUrl;
        }

        public String getNotificationsUrl() {
            return notificationsUrl;
        }

        public void setNotificationsUrl(String notificationsUrl) {
            this.notificationsUrl = notificationsUrl;
        }
    }
}
