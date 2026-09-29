package com.library.book.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    /** Блокировка строки на время выдачи: два одновременных запроса не заберут одну книгу. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Book> findWithLockById(UUID id);
}
