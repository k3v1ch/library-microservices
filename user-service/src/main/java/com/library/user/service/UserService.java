package com.library.user.service;

import com.library.common.api.ApiException;
import com.library.common.util.Pii;
import com.library.user.domain.User;
import com.library.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User register(UUID userId, String fullName, String email, String phone) {
        if (users.existsByUserId(userId)) {
            throw ApiException.conflict("user_exists", "Профиль читателя уже создан");
        }
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("email_exists", "Читатель с таким e-mail уже зарегистрирован");
        }
        User reader = users.save(new User(userId, fullName, email, phone));
        // В лог попадает только маскированный e-mail: персональные данные не утекают в логи.
        log.info("Зарегистрирован читатель id={} карта={} email={}",
                reader.getId(), reader.getCardNumber(), Pii.maskEmail(email));
        return reader;
    }

    @Transactional(readOnly = true)
    public User require(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> ApiException.notFound("user_not_found", "Читатель не найден: " + id));
    }

    @Transactional(readOnly = true)
    public User requireByUserId(UUID userId) {
        return users.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("user_not_found",
                        "Профиль читателя не создан для пользователя " + userId));
    }

    @Transactional(readOnly = true)
    public Page<User> list(User.Status status, Pageable pageable) {
        return status == null ? users.findAll(pageable) : users.findByStatus(status, pageable);
    }

    @Transactional
    public User updateContacts(UUID id, String fullName, String email, String phone) {
        User reader = require(id);
        reader.updateContacts(fullName, email, phone);
        return reader;
    }

    @Transactional
    public User block(UUID id, String reason) {
        User reader = require(id);
        reader.block(reason);
        log.warn("Читатель заблокирован id={} причина={}", id, reason);
        return reader;
    }

    @Transactional
    public User unblock(UUID id) {
        User reader = require(id);
        reader.unblock();
        log.info("Блокировка снята id={}", id);
        return reader;
    }
}
