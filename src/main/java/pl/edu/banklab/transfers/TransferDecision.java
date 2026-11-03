package pl.edu.banklab.transfers;

public enum TransferDecision {
    ACCEPTED,
    INVALID_AMOUNT,
    SAME_ACCOUNT,
    ACCOUNT_INACTIVE,
    INSUFFICIENT_FUNDS,
    DAILY_LIMIT_EXCEEDED
}