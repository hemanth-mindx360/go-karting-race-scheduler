package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RaceHistory {
    private final String batchId;
    private final List<BookingToken> tokens;
    private final LocalDateTime completedAt;
    private final double revenue;

    public RaceHistory(RaceBatch batch) {
        this.batchId = batch.getBatchId();
        this.tokens = new ArrayList<BookingToken>(batch.getTokens());
        this.completedAt = batch.getEndedAt() == null ? LocalDateTime.now() : batch.getEndedAt();
        double total = 0;
        for (BookingToken token : tokens) total += token.getTotalPrice();
        this.revenue = total;
    }

    public String getBatchId() { return batchId; }
    public List<BookingToken> getTokens() { return tokens; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public double getRevenue() { return revenue; }
}
