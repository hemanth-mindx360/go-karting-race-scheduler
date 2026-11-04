package models;

import enums.KartType;
import enums.TokenStatus;
import java.time.LocalDateTime;

public class BookingToken {
    private final String tokenId;
    private final Customer customer;
    private final int groupSize;
    private final int laps;
    private final KartType kartType;
    private final double totalPrice;
    private final LocalDateTime createdAt;
    private volatile TokenStatus status;

    public BookingToken(String tokenId, Customer customer, int groupSize, int laps,
                        KartType kartType, double totalPrice) {
        this.tokenId = tokenId;
        this.customer = customer;
        this.groupSize = groupSize;
        this.laps = laps;
        this.kartType = kartType;
        this.totalPrice = totalPrice;
        this.createdAt = LocalDateTime.now();
        this.status = TokenStatus.WAITING;
    }

    public String getTokenId() { return tokenId; }
    public Customer getCustomer() { return customer; }
    public int getGroupSize() { return groupSize; }
    public int getLaps() { return laps; }
    public KartType getKartType() { return kartType; }
    public double getTotalPrice() { return totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }

    public String toString() {
        return tokenId + " | " + customer.getName() + " | " + groupSize
                + " riders | " + kartType + " | " + laps + " laps | "
                + status + " | Rs." + String.format("%.2f", totalPrice);
    }
}
