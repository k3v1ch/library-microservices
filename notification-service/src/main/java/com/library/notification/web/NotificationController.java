package com.library.notification.web;

import com.library.common.event.BorrowEvent;
import com.library.notification.domain.Notification;
import com.library.notification.domain.NotificationRepository;
import com.library.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Уведомления")
@RestController
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notifications;

    public NotificationController(NotificationService notificationService, NotificationRepository notifications) {
        this.notificationService = notificationService;
        this.notifications = notifications;
    }

    /** Приём события из outbox сервиса выдачи (режим без Kafka). */
    @Operation(summary = "Внутренний приём доменного события (scope=internal)")
    @PostMapping("/internal/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@Valid @RequestBody BorrowEvent event) {
        notificationService.handle(event);
    }

    @Operation(summary = "Журнал уведомлений (библиотекарь)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/notifications")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public List<NotificationResponse> list(@RequestParam(required = false) UUID userId,
                                           @PageableDefault(size = 50) Pageable pageable) {
        Page<Notification> page = userId == null
                ? notifications.findAllByOrderByCreatedAtDesc(pageable)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.getContent().stream().map(NotificationResponse::from).toList();
    }

    public record NotificationResponse(UUID id, UUID eventId, String eventType, UUID userId,
                                       String channel, String subject, String body,
                                       String status, String error, Instant createdAt) {

        static NotificationResponse from(Notification notification) {
            return new NotificationResponse(notification.getId(), notification.getEventId(),
                    notification.getEventType(), notification.getUserId(), notification.getChannel(),
                    notification.getSubject(), notification.getBody(), notification.getStatus().name(),
                    notification.getError(), notification.getCreatedAt());
        }
    }
}
