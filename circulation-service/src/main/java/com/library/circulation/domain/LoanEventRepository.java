package com.library.circulation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanEventRepository extends JpaRepository<LoanEventRecord, Long> {

    List<LoanEventRecord> findByLoanIdOrderBySeqAsc(UUID loanId);

    int countByLoanId(UUID loanId);
}
