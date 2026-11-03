package services;

import enums.BatchStatus;
import enums.KartType;
import enums.TokenStatus;
import exceptions.DuplicateTokenException;
import exceptions.IllegalAdminActionException;
import exceptions.InvalidGroupSizeException;
import exceptions.InvalidKartTypeException;
import exceptions.InvalidTokenStateException;
import exceptions.RaceEndException;
import exceptions.RaceStartException;
import exceptions.TokenNotFoundException;
import exceptions.TrackCapacityExceededException;
import models.BookingToken;
import models.Customer;
import models.RaceBatch;
import models.RaceHistory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ArenaService {
    public static final int MAX_GROUP_SIZE = 6;
    public static final int MAX_TRACK_KARTS = 15;
    private final Map<String, Customer> customers = new HashMap<String, Customer>();
    private final Map<String, BookingToken> tokens = new HashMap<String, BookingToken>();
    private final Deque<BookingToken> waitingQueue = new ArrayDeque<BookingToken>();
    private final List<RaceBatch> batches = new ArrayList<RaceBatch>();
    private final List<RaceHistory> history = new ArrayList<RaceHistory>();
    private int tokenSequence = 1000;
    private int batchSequence = 1;
    private RaceBatch activeRace;

    public synchronized Customer registerCustomer(String username, String password, String name) {
        if (customers.containsKey(username)) throw new IllegalArgumentException("Username is already registered");
        Customer customer = new Customer(username, password, name);
        customers.put(username, customer);
        return customer;
    }

    public synchronized Customer loginCustomer(String username, String password) {
        Customer customer = customers.get(username);
        if (customer == null || !customer.getPassword().equals(password)) return null;
        return customer;
    }

    public synchronized BookingToken book(Customer customer, int groupSize, int laps, KartType kartType) {
        if (customer == null || !customers.containsKey(customer.getUsername())) {
            throw new IllegalArgumentException("Customer must be registered");
        }
        if (groupSize < 1 || groupSize > MAX_GROUP_SIZE) {
            throw new InvalidGroupSizeException("Group size must be between 1 and " + MAX_GROUP_SIZE);
        }
        if (kartType == null) throw new InvalidKartTypeException("Kart type is required");

        double unitPrice;
        try {
            unitPrice = kartType.priceForLaps(laps);
        } catch (IllegalArgumentException ex) {
            throw new InvalidKartTypeException(ex.getMessage());
        }

        for (BookingToken oldToken : customer.getBookings()) {
            if (oldToken.getStatus() != TokenStatus.COMPLETED
                    && oldToken.getStatus() != TokenStatus.CANCELLED) {
                throw new DuplicateTokenException("Customer already has an active token");
            }
        }

        tokenSequence++;
        BookingToken token = new BookingToken("T-" + tokenSequence, customer, groupSize,
                laps, kartType, groupSize * unitPrice);
        tokens.put(token.getTokenId(), token);
        waitingQueue.addLast(token);
        customer.addBooking(token);
        System.out.println("[LOG] Booking created: " + token.getTokenId());
        return token;
    }

    public synchronized List<RaceBatch> scheduleWaitingTokens() {
        while (!waitingQueue.isEmpty()) {
            if (waitingQueue.peekFirst().getGroupSize() > MAX_TRACK_KARTS) {
                throw new TrackCapacityExceededException("Token exceeds track capacity");
            }
            RaceBatch batch = new RaceBatch("B-" + batchSequence++);
            while (!waitingQueue.isEmpty()
                    && batch.getKartCount() + waitingQueue.peekFirst().getGroupSize() <= MAX_TRACK_KARTS) {
                BookingToken token = waitingQueue.removeFirst();
                token.setStatus(TokenStatus.SCHEDULED);
                batch.addToken(token);
            }
            batches.add(batch);
            System.out.println("[LOG] Batch scheduled: " + batch.getBatchId());
        }
        return getScheduledBatches();
    }

    public synchronized RaceBatch startNextRace() {
        if (activeRace != null) throw new RaceStartException("A race is already active");
        for (RaceBatch batch : batches) {
            if (batch.getStatus() == BatchStatus.SCHEDULED) {
                activeRace = batch;
                batch.markStarted();
                for (BookingToken token : batch.getTokens()) token.setStatus(TokenStatus.RACING);
                System.out.println("[LOG] Race started: " + batch.getBatchId());
                return batch;
            }
        }
        throw new RaceStartException("No scheduled batch available");
    }

    public synchronized RaceHistory endActiveRace() {
        if (activeRace == null) throw new RaceEndException("No active race is running");
        RaceBatch completed = activeRace;
        for (BookingToken token : completed.getTokens()) token.setStatus(TokenStatus.COMPLETED);
        completed.markEnded();
        RaceHistory result = new RaceHistory(completed);
        history.add(result);
        activeRace = null;
        System.out.println("[LOG] Race ended: " + completed.getBatchId());
        return result;
    }

    public synchronized void cancelToken(String tokenId) {
        cancelToken(tokenId, null);
    }

    public synchronized void cancelToken(String tokenId, Customer customer) {
        BookingToken token = getToken(tokenId);
        if (customer != null && token.getCustomer() != customer) {
            throw new IllegalAdminActionException("This token does not belong to the customer");
        }
        if (token.getStatus() == TokenStatus.COMPLETED || token.getStatus() == TokenStatus.CANCELLED) {
            throw new IllegalAdminActionException("Completed or cancelled tokens cannot be modified");
        }
        if (token.getStatus() == TokenStatus.RACING) {
            throw new InvalidTokenStateException("A racing token cannot be cancelled");
        }
        waitingQueue.remove(token);
        Iterator<RaceBatch> iterator = batches.iterator();
        while (iterator.hasNext()) {
            RaceBatch batch = iterator.next();
            if (batch.getStatus() == BatchStatus.SCHEDULED) {
                batch.removeToken(token);
                if (batch.getTokens().isEmpty()) iterator.remove();
            }
        }
        token.setStatus(TokenStatus.CANCELLED);
        System.out.println("[LOG] Token cancelled: " + tokenId);
    }

    public synchronized void reprioritizeToken(String tokenId) {
        BookingToken token = getToken(tokenId);
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new InvalidTokenStateException("Only waiting tokens can be reprioritized");
        }
        waitingQueue.remove(token);
        waitingQueue.addFirst(token);
        System.out.println("[LOG] Token reprioritized: " + tokenId);
    }

    public synchronized BookingToken getToken(String tokenId) {
        BookingToken token = tokens.get(tokenId);
        if (token == null) throw new TokenNotFoundException("Token not found: " + tokenId);
        return token;
    }

    public synchronized List<BookingToken> getWaitingTokens() {
        return new ArrayList<BookingToken>(waitingQueue);
    }

    public synchronized List<RaceBatch> getScheduledBatches() {
        List<RaceBatch> result = new ArrayList<RaceBatch>();
        for (RaceBatch batch : batches) {
            if (batch.getStatus() == BatchStatus.SCHEDULED) result.add(batch);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized List<RaceBatch> getAllBatches() {
        return new ArrayList<RaceBatch>(batches);
    }

    public synchronized List<RaceHistory> getHistory() {
        return new ArrayList<RaceHistory>(history);
    }

    public synchronized RaceBatch getActiveRace() { return activeRace; }

    public synchronized double getTotalRevenue() {
        double total = 0;
        for (RaceHistory item : history) total += item.getRevenue();
        return total;
    }
}
