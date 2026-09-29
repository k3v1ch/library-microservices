package com.library.catalog.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    /** Блокировка строки на время изменения счётчика: два одновременных запроса не заберут один экземпляр. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Book> findWithLockById(UUID id);
}
