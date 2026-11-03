package models;
 
import enums.BatchStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
public class RaceBatch {
    private final String batchId;
    private final List<BookingToken> tokens = new ArrayList<>();
    private BatchStatus status = BatchStatus.SCHEDULED;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
 
    public RaceBatch(String batchId) {
        this.batchId = batchId;
    }
    public String getBatchId() {
        return batchId;
    }
    public List<BookingToken> getTokens() {
        return Collections.unmodifiableList(tokens);
    }
    public int getKartCount() {
        int count = 0;
        for (BookingToken token : tokens) count += token.getGroupSize();
        return count;
    }
    public BatchStatus getStatus() {
        return status;
    }
 
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    public LocalDateTime getEndedAt() {
        return endedAt;
    }
    public void addToken(BookingToken token) {
        tokens.add(token);
    }
    public void removeToken(BookingToken token) {
        tokens.remove(token);
    }
    public void setStatus(BatchStatus status) {
        this.status = status;
    }
    public void markStarted() {
        startedAt = LocalDateTime.now();
        status = BatchStatus.RACING;
    }
    public void markEnded() {
        endedAt = LocalDateTime.now(); status = BatchStatus.COMPLETED;
    }
}
 
 