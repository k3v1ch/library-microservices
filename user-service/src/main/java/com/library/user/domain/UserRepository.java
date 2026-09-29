package com.library.user.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByEmail(String email);

    Page<User> findByStatus(User.Status status, Pageable pageable);
}
