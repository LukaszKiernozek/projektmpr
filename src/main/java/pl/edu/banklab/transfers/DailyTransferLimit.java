package pl.edu.banklab.transfers;

public class DailyTransferLimit {
    private long remainingInCents;

    public DailyTransferLimit(long remainingInCents) {
        this.remainingInCents = remainingInCents;
    }

    public long remainingInCents() {
        return remainingInCents;
    }

    public void consume(long amountInCents) {
        if (amountInCents > remainingInCents) {
            throw new IllegalStateException("Daily limit exceeded");
        }
        remainingInCents -= amountInCents;
    }
}