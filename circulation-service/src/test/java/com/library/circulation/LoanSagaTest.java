package com.library.circulation;

import com.library.circulation.client.CatalogClient;
import com.library.circulation.client.ReadersClient;
import com.library.circulation.domain.Loan;
import com.library.circulation.domain.LoanStatus;
import com.library.circulation.domain.OutboxRepository;
import com.library.circulation.service.LoanService;
import com.library.circulation.service.LoanStore;
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
@SpringBootTest(properties = "circulation.scheduling-enabled=false")
class LoanSagaTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanStore store;

    @Autowired
    private OutboxRepository outbox;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private ReadersClient readersClient;

    private UUID readerId;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        readerId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        when(readersClient.status(any())).thenReturn(new ReadersClient.ReaderStatusView(readerId, "ACTIVE", true));
        when(catalogClient.reserveCopy(any(), any())).thenAnswer(invocation ->
                new CatalogClient.ReservationView(invocation.getArgument(0), invocation.getArgument(1),
                        "ACTIVE", "Чистый код"));
    }

    @Test
    void successfulIssueActivatesLoanAndWritesEventToOutbox() {
        Loan loan = loanService.issue(readerId, bookId, null, "librarian");

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getBookTitle()).isEqualTo("Чистый код");
        assertThat(loan.getDueDate()).isNotNull();
        verify(catalogClient).reserveCopy(loan.getId(), bookId);

        assertThat(outbox.findAll())
                .anyMatch(message -> message.getAggregateId().equals(loan.getId())
                        && message.getType().equals(EventType.LOAN_ISSUED));
        assertThat(store.history(loan.getId())).extracting("type").containsExactly("REQUESTED", "ISSUED");
    }

    @Test
    void blockedReaderIsRejectedBeforeAnyReservation() {
        when(readersClient.status(any())).thenReturn(new ReadersClient.ReaderStatusView(readerId, "BLOCKED", false));

        assertThatThrownBy(() -> loanService.issue(readerId, bookId, null, "librarian"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("нельзя выдавать");
    }

    @Test
    void catalogFailureRollsBackSagaAndReleasesCopy() {
        when(catalogClient.reserveCopy(any(), any()))
                .thenThrow(ApiException.unavailable("catalog_unavailable", "Каталог недоступен"));

        assertThatThrownBy(() -> loanService.issue(readerId, bookId, null, "librarian"))
                .isInstanceOf(ApiException.class);

        Loan rejected = store.byReader(readerId, org.springframework.data.domain.PageRequest.of(0, 5))
                .getContent().getFirst();
        assertThat(rejected.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(rejected.getRejectReason()).contains("Каталог недоступен");
        // Компенсация: бронь освобождается, даже если её не успели создать (вызов идемпотентен).
        verify(catalogClient).releaseCopy(rejected.getId());
    }

    @Test
    void returnClosesLoanPublishesEventAndReleasesCopy() {
        Loan loan = loanService.issue(readerId, bookId, null, "librarian");

        Loan returned = loanService.returnBook(loan.getId(), "librarian");

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        verify(catalogClient).releaseCopy(loan.getId());
        assertThat(store.require(loan.getId()).isCopyReleasePending()).isFalse();
        assertThat(outbox.findAll())
                .anyMatch(message -> message.getAggregateId().equals(loan.getId())
                        && message.getType().equals(EventType.LOAN_RETURNED));
    }

    @Test
    void unreachableCatalogOnReturnLeavesCopyReleaseToScheduler() {
        Loan loan = loanService.issue(readerId, bookId, null, "librarian");
        doThrow(ApiException.unavailable("catalog_unavailable", "Каталог недоступен"))
                .when(catalogClient).releaseCopy(loan.getId());

        Loan returned = loanService.returnBook(loan.getId(), "librarian");

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(store.require(loan.getId()).isCopyReleasePending()).isTrue();
    }

    @Test
    void repeatedRequestWithSameIdempotencyKeyReturnsSameLoan() {
        String key = UUID.randomUUID().toString();

        Loan first = loanService.issue(readerId, bookId, key, "librarian");
        Loan second = loanService.issue(readerId, bookId, key, "librarian");

        assertThat(second.getId()).isEqualTo(first.getId());
        verify(catalogClient).reserveCopy(first.getId(), bookId);
    }

    @Test
    void loanLimitIsEnforced() {
        for (int i = 0; i < 3; i++) {
            loanService.issue(readerId, UUID.randomUUID(), null, "librarian");
        }

        assertThatThrownBy(() -> loanService.issue(readerId, UUID.randomUUID(), null, "librarian"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("лимит");
    }
}
