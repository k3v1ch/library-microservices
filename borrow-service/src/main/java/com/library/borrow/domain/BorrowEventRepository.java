package com.library.borrow.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BorrowEventRepository extends JpaRepository<BorrowEventRecord, Long> {

    List<BorrowEventRecord> findByBorrowIdOrderBySeqAsc(UUID borrowId);

    int countByBorrowId(UUID borrowId);
}
