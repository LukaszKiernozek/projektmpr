package pl.edu.banklab.transfers;

public class TransferService {
    private final AccountFunds funds;
    private final DailyTransferLimit limit;

    public TransferService(AccountFunds funds, DailyTransferLimit limit) {
        this.funds = funds;
        this.limit = limit;
    }

    public TransferDecision accept(RequestTransferCommand command) {
        if (command.amountInCents() <= 0) {
            return TransferDecision.INVALID_AMOUNT;
        }
        if (funds.accountId().equals(command.targetAccountId())) {
            return TransferDecision.SAME_ACCOUNT;
        }
        if (!funds.active()) {
            return TransferDecision.ACCOUNT_INACTIVE;
        }
        if (funds.availableInCents() < command.amountInCents()) {
            return TransferDecision.INSUFFICIENT_FUNDS;
        }
        if (limit.remainingInCents() < command.amountInCents()) {
            return TransferDecision.DAILY_LIMIT_EXCEEDED;
        }

        funds.reserve(command.amountInCents());
        limit.consume(command.amountInCents());
        return TransferDecision.ACCEPTED;
    }
}