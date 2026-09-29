package com.library.circulation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /** Порция неотправленных событий: порядок по id = порядок появления. */
    List<OutboxMessage> findTop50ByPublishedAtIsNullAndAttemptsLessThanOrderByIdAsc(int maxAttempts);

    long countByPublishedAtIsNull();

    boolean existsByEventId(UUID eventId);
}
