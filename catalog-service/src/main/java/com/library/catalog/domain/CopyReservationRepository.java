package com.library.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CopyReservationRepository extends JpaRepository<CopyReservation, UUID> {

    Optional<CopyReservation> findByLoanId(UUID loanId);

    long countByBookIdAndStatus(UUID bookId, CopyReservation.Status status);
}
