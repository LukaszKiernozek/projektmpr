package pl.edu.banklab.transfers;

public class AccountFunds {
    private final String accountId;
    private final boolean active;
    private long availableInCents;

    public AccountFunds(String accountId, boolean active, long availableInCents) {
        this.accountId = accountId;
        this.active = active;
        this.availableInCents = availableInCents;
    }

    public String accountId() {
        return accountId;
    }

    public boolean active() {
        return active;
    }

    public long availableInCents() {
        return availableInCents;
    }

    public void reserve(long amountInCents) {
        if (amountInCents > availableInCents) {
            throw new IllegalStateException("Insufficient funds");
        }
        availableInCents -= amountInCents;
    }
}