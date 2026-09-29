package com.library.readers.service;

import com.library.common.api.ApiException;
import com.library.common.util.Pii;
import com.library.readers.domain.Reader;
import com.library.readers.domain.ReaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReaderService {

    private static final Logger log = LoggerFactory.getLogger(ReaderService.class);

    private final ReaderRepository readers;

    public ReaderService(ReaderRepository readers) {
        this.readers = readers;
    }

    @Transactional
    public Reader register(UUID userId, String fullName, String email, String phone) {
        if (readers.existsByUserId(userId)) {
            throw ApiException.conflict("reader_exists", "Профиль читателя уже создан");
        }
        if (readers.existsByEmail(email)) {
            throw ApiException.conflict("email_exists", "Читатель с таким e-mail уже зарегистрирован");
        }
        Reader reader = readers.save(new Reader(userId, fullName, email, phone));
        // В лог попадает только маскированный e-mail: персональные данные не утекают в логи.
        log.info("Зарегистрирован читатель id={} карта={} email={}",
                reader.getId(), reader.getCardNumber(), Pii.maskEmail(email));
        return reader;
    }

    @Transactional(readOnly = true)
    public Reader require(UUID id) {
        return readers.findById(id)
                .orElseThrow(() -> ApiException.notFound("reader_not_found", "Читатель не найден: " + id));
    }

    @Transactional(readOnly = true)
    public Reader requireByUserId(UUID userId) {
        return readers.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("reader_not_found",
                        "Профиль читателя не создан для пользователя " + userId));
    }

    @Transactional(readOnly = true)
    public Page<Reader> list(Reader.Status status, Pageable pageable) {
        return status == null ? readers.findAll(pageable) : readers.findByStatus(status, pageable);
    }

    @Transactional
    public Reader updateContacts(UUID id, String fullName, String email, String phone) {
        Reader reader = require(id);
        reader.updateContacts(fullName, email, phone);
        return reader;
    }

    @Transactional
    public Reader block(UUID id, String reason) {
        Reader reader = require(id);
        reader.block(reason);
        log.warn("Читатель заблокирован id={} причина={}", id, reason);
        return reader;
    }

    @Transactional
    public Reader unblock(UUID id) {
        Reader reader = require(id);
        reader.unblock();
        log.info("Блокировка снята id={}", id);
        return reader;
    }
}
