package pl.edu.banklab.transfers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransferServiceTest {
    private final AccountFunds funds = new AccountFunds("ACC-1", true, 100_000L);
    private final DailyTransferLimit limit = new DailyTransferLimit(60_000L);
    private final TransferService service = new TransferService(funds, limit);

    private RequestTransferCommand transfer(long amount) {
        return new RequestTransferCommand("ACC-2", amount);
    }

    private void assertUnchanged() {
        assertAll(
                () -> assertEquals(100_000L, funds.availableInCents()),
                () -> assertEquals(60_000L, limit.remainingInCents())
        );
    }

    @Test
    void acceptsTransferAndReservesFundsAndLimit() {
        var result = service.accept(transfer(25_000L));
        assertAll(
                () -> assertEquals(TransferDecision.ACCEPTED, result),
                () -> assertEquals(75_000L, funds.availableInCents()),
                () -> assertEquals(35_000L, limit.remainingInCents())
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -10_000L})
    void rejectsNonPositiveAmountWithoutChangingState(long amount) {
        assertEquals(TransferDecision.INVALID_AMOUNT, service.accept(transfer(amount)));
        assertUnchanged();
    }

    @Test
    void rejectsTransferToTheSameAccountWithoutChangingState() {
        var command = new RequestTransferCommand("ACC-1", 10_000L);
        assertEquals(TransferDecision.SAME_ACCOUNT, service.accept(command));
        assertUnchanged();
    }

    @Test
    void rejectsInactiveSourceAccountWithoutChangingState() {
        var inactive = new AccountFunds("ACC-1", false, 100_000L);
        var otherService = new TransferService(inactive, limit);
        assertEquals(TransferDecision.ACCOUNT_INACTIVE,
                otherService.accept(transfer(10_000L)));
        assertAll(
                () -> assertEquals(100_000L, inactive.availableInCents()),
                () -> assertEquals(60_000L, limit.remainingInCents())
        );
    }

    @Test
    void rejectsInsufficientFundsBeforeCheckingLimit() {
        // Kwota przekracza jednocześnie środki i limit.
        assertEquals(TransferDecision.INSUFFICIENT_FUNDS,
                service.accept(transfer(100_001L)));
        assertUnchanged();
    }

    @Test
    void rejectsExceededDailyLimitWithoutReservingFunds() {
        assertEquals(TransferDecision.DAILY_LIMIT_EXCEEDED,
                service.accept(transfer(60_001L)));
        assertUnchanged();
    }

    @Test
    void acceptsAmountEqualToRemainingLimit() {
        assertEquals(TransferDecision.ACCEPTED, service.accept(transfer(60_000L)));
        assertAll(
                () -> assertEquals(40_000L, funds.availableInCents()),
                () -> assertEquals(0L, limit.remainingInCents())
        );
    }

    @Test
    void acceptsAmountEqualToAvailableFunds() {
        var smallAccount = new AccountFunds("ACC-1", true, 20_000L);
        var otherService = new TransferService(smallAccount, limit);
        assertEquals(TransferDecision.ACCEPTED,
                otherService.accept(transfer(20_000L)));
        assertAll(
                () -> assertEquals(0L, smallAccount.availableInCents()),
                () -> assertEquals(40_000L, limit.remainingInCents())
        );
    }

    @Test
    void subsequentTransferUsesRemainingLimit() {
        assertEquals(TransferDecision.ACCEPTED, service.accept(transfer(40_000L)));
        assertEquals(TransferDecision.DAILY_LIMIT_EXCEEDED,
                service.accept(transfer(25_000L)));
        assertAll(
                () -> assertEquals(60_000L, funds.availableInCents()),
                () -> assertEquals(20_000L, limit.remainingInCents())
        );
    }

    @Test
    void subsequentTransferUsesRemainingFunds() {
        var smallAccount = new AccountFunds("ACC-1", true, 30_000L);
        var otherService = new TransferService(smallAccount, limit);
        assertEquals(TransferDecision.ACCEPTED,
                otherService.accept(transfer(20_000L)));
        assertEquals(TransferDecision.INSUFFICIENT_FUNDS,
                otherService.accept(transfer(15_000L)));
        assertAll(
                () -> assertEquals(10_000L, smallAccount.availableInCents()),
                () -> assertEquals(40_000L, limit.remainingInCents())
        );
    }

    @Test
    void validatesAmountBeforeAccountIdentity() {
        var command = new RequestTransferCommand("ACC-1", 0L);
        assertEquals(TransferDecision.INVALID_AMOUNT, service.accept(command));
        assertUnchanged();
    }

    @Test
    void validatesAccountIdentityBeforeActivity() {
        var inactive = new AccountFunds("ACC-1", false, 100_000L);
        var otherService = new TransferService(inactive, limit);
        assertEquals(TransferDecision.SAME_ACCOUNT,
                otherService.accept(new RequestTransferCommand("ACC-1", 10_000L)));
        assertEquals(100_000L, inactive.availableInCents());
        assertEquals(60_000L, limit.remainingInCents());
    }

    @Test
    void validatesActivityBeforeFunds() {
        var inactive = new AccountFunds("ACC-1", false, 100_000L);
        var otherService = new TransferService(inactive, limit);
        assertEquals(TransferDecision.ACCOUNT_INACTIVE,
                otherService.accept(transfer(100_001L)));
        assertEquals(100_000L, inactive.availableInCents());
        assertEquals(60_000L, limit.remainingInCents());
    }
}