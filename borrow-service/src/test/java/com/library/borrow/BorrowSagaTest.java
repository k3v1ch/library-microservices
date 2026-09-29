package com.library.borrow;

import com.library.borrow.client.BookClient;
import com.library.borrow.client.UserClient;
import com.library.borrow.domain.BorrowRecord;
import com.library.borrow.domain.BorrowStatus;
import com.library.borrow.domain.OutboxRepository;
import com.library.borrow.service.BorrowService;
import com.library.borrow.service.BorrowStore;
import com.library.common.api.ApiException;
import com.library.common.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Сага выдачи целиком: успешный путь, два вида отказа и компенсация. */
@SpringBootTest(properties = "borrow.scheduling-enabled=false")
class BorrowSagaTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BorrowStore store;

    @Autowired
    private OutboxRepository outbox;

    @MockitoBean
    private BookClient bookClient;

    @MockitoBean
    private UserClient userClient;

    private UUID userId;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        when(userClient.status(any())).thenReturn(new UserClient.UserStatusView(userId, "ACTIVE", true));
        when(bookClient.reserveCopy(any(), any())).thenAnswer(invocation ->
                new BookClient.ReservationView(invocation.getArgument(0), invocation.getArgument(1),
                        "ACTIVE", "Чистый код"));
    }

    @Test
    void successfulIssueActivatesBorrowRecordAndWritesEventToOutbox() {
        BorrowRecord borrow = borrowService.issue(userId, bookId, null, "librarian");

        assertThat(borrow.getStatus()).isEqualTo(BorrowStatus.ACTIVE);
        assertThat(borrow.getBookTitle()).isEqualTo("Чистый код");
        assertThat(borrow.getDueDate()).isNotNull();
        verify(bookClient).reserveCopy(borrow.getId(), bookId);

        assertThat(outbox.findAll())
                .anyMatch(message -> message.getAggregateId().equals(borrow.getId())
                        && message.getType().equals(EventType.BORROW_ISSUED));
        assertThat(store.history(borrow.getId())).extracting("type").containsExactly("REQUESTED", "ISSUED");
    }

    @Test
    void blockedUserIsRejectedBeforeAnyReservation() {
        when(userClient.status(any())).thenReturn(new UserClient.UserStatusView(userId, "BLOCKED", false));

        assertThatThrownBy(() -> borrowService.issue(userId, bookId, null, "librarian"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("нельзя выдавать");
    }

    @Test
    void catalogFailureRollsBackSagaAndReleasesCopy() {
        when(bookClient.reserveCopy(any(), any()))
                .thenThrow(ApiException.unavailable("book_service_unavailable", "Каталог недоступен"));

        assertThatThrownBy(() -> borrowService.issue(userId, bookId, null, "librarian"))
                .isInstanceOf(ApiException.class);

        BorrowRecord rejected = store.byUser(userId, org.springframework.data.domain.PageRequest.of(0, 5))
                .getContent().getFirst();
        assertThat(rejected.getStatus()).isEqualTo(BorrowStatus.REJECTED);
        assertThat(rejected.getRejectReason()).contains("Каталог недоступен");
        // Компенсация: бронь освобождается, даже если её не успели создать (вызов идемпотентен).
        verify(bookClient).releaseCopy(rejected.getId());
    }

    @Test
    void returnClosesBorrowRecordPublishesEventAndReleasesCopy() {
        BorrowRecord borrow = borrowService.issue(userId, bookId, null, "librarian");

        BorrowRecord returned = borrowService.returnBook(borrow.getId(), "librarian");

        assertThat(returned.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        verify(bookClient).releaseCopy(borrow.getId());
        assertThat(store.require(borrow.getId()).isCopyReleasePending()).isFalse();
        assertThat(outbox.findAll())
                .anyMatch(message -> message.getAggregateId().equals(borrow.getId())
                        && message.getType().equals(EventType.BORROW_RETURNED));
    }

    @Test
    void unreachableCatalogOnReturnLeavesCopyReleaseToScheduler() {
        BorrowRecord borrow = borrowService.issue(userId, bookId, null, "librarian");
        doThrow(ApiException.unavailable("book_service_unavailable", "Каталог недоступен"))
                .when(bookClient).releaseCopy(borrow.getId());

        BorrowRecord returned = borrowService.returnBook(borrow.getId(), "librarian");

        assertThat(returned.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(store.require(borrow.getId()).isCopyReleasePending()).isTrue();
    }

    @Test
    void repeatedRequestWithSameIdempotencyKeyReturnsSameBorrowRecord() {
        String key = UUID.randomUUID().toString();

        BorrowRecord first = borrowService.issue(userId, bookId, key, "librarian");
        BorrowRecord second = borrowService.issue(userId, bookId, key, "librarian");

        assertThat(second.getId()).isEqualTo(first.getId());
        verify(bookClient).reserveCopy(first.getId(), bookId);
    }

    @Test
    void borrowLimitIsEnforced() {
        for (int i = 0; i < 3; i++) {
            borrowService.issue(userId, UUID.randomUUID(), null, "librarian");
        }

        assertThatThrownBy(() -> borrowService.issue(userId, UUID.randomUUID(), null, "librarian"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("лимит");
    }
}
