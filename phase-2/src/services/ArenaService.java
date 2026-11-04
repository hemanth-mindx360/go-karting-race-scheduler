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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class ArenaService {
    public static final int MAX_GROUP_SIZE = 6;
    public static final int MAX_TRACK_KARTS = 15;
    private final Map<String, Customer> customers = new ConcurrentHashMap<String, Customer>();
    private final Map<String, BookingToken> tokens = new ConcurrentHashMap<String, BookingToken>();
    private final ConcurrentLinkedQueue<BookingToken> waitingQueue =
            new ConcurrentLinkedQueue<BookingToken>();
    private final List<RaceBatch> batches =
            Collections.synchronizedList(new ArrayList<RaceBatch>());
    private final List<RaceHistory> history =
            Collections.synchronizedList(new ArrayList<RaceHistory>());
    private final AtomicInteger tokenSequence = new AtomicInteger(1000);
    private final AtomicInteger batchSequence = new AtomicInteger(1);
    private final AtomicInteger kartsInUse = new AtomicInteger(0);
    private final AtomicReference<RaceBatch> activeRace = new AtomicReference<RaceBatch>();
    private final ReentrantLock schedulingLock = new ReentrantLock();
    private final ScheduledExecutorService background =
            Executors.newScheduledThreadPool(3);
    private final NotificationService notifications = new NotificationService();
    private final long tokenExpirySeconds;
    private final long autoStartSeconds;
    private ScheduledFuture<?> schedulerTask;
    private ScheduledFuture<?> expiryTask;
    private ScheduledFuture<?> autoStartTask;

    public ArenaService() {
        this(300, 30);
    }

    public ArenaService(long tokenExpirySeconds, long autoStartSeconds) {
        this.tokenExpirySeconds = tokenExpirySeconds;
        this.autoStartSeconds = autoStartSeconds;
    }

    public synchronized Customer registerCustomer(String username, String password, String name) {
        if (customers.containsKey(username)) {
            throw new IllegalArgumentException("Username is already registered");
        }
        Customer customer = new Customer(username, password, name);
        customers.put(username, customer);
        return customer;
    }

    public Customer loginCustomer(String username, String password) {
        Customer customer = customers.get(username);
        if (customer == null || !customer.getPassword().equals(password)) return null;
        return customer;
    }

    public synchronized BookingToken book(Customer customer, int groupSize, int laps,
                                           KartType kartType) {
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
                    && oldToken.getStatus() != TokenStatus.CANCELLED
                    && oldToken.getStatus() != TokenStatus.EXPIRED) {
                throw new DuplicateTokenException("Customer already has an active token");
            }
        }

        String tokenId = "T-" + tokenSequence.incrementAndGet();
        BookingToken token = new BookingToken(tokenId, customer, groupSize, laps,
                kartType, groupSize * unitPrice);
        tokens.put(tokenId, token);
        waitingQueue.add(token);
        customer.addBooking(token);
        System.out.println("[LOG] Booking created: " + tokenId
                + " by " + Thread.currentThread().getName());
        return token;
    }

    public void startBackgroundScheduler() {
        synchronized (this) {
            if (schedulerTask != null && !schedulerTask.isCancelled()) return;
            schedulerTask = background.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    scheduleWaitingTokens();
                }
            }, 0, 1, TimeUnit.SECONDS);
            expiryTask = background.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    expireWaitingTokens();
                }
            }, 1, 1, TimeUnit.SECONDS);
            autoStartTask = background.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        startNextRace();
                    } catch (RaceStartException ignored) {
                        // There may be no batch ready on a scheduler tick.
                    }
                }
            }, autoStartSeconds, autoStartSeconds, TimeUnit.SECONDS);
        }
        System.out.println("[LOG] Background scheduler started.");
    }

    public synchronized void stopBackgroundScheduler() {
        if (schedulerTask != null) schedulerTask.cancel(false);
        if (expiryTask != null) expiryTask.cancel(false);
        if (autoStartTask != null) autoStartTask.cancel(false);
        System.out.println("[LOG] Background scheduler stopped.");
    }

    public void shutdown() {
        stopBackgroundScheduler();
        background.shutdownNow();
    }

    public List<RaceBatch> scheduleWaitingTokens() {
        schedulingLock.lock();
        try {
            List<RaceBatch> newBatches = new ArrayList<RaceBatch>();
            while (!waitingQueue.isEmpty()) {
                BookingToken first = waitingQueue.peek();
                if (first == null) break;
                if (first.getGroupSize() > MAX_TRACK_KARTS) {
                    waitingQueue.poll();
                    first.setStatus(TokenStatus.CANCELLED);
                    throw new TrackCapacityExceededException("Token exceeds track capacity");
                }
                RaceBatch batch = new RaceBatch("B-" + batchSequence.getAndIncrement());
                while (true) {
                    BookingToken token = waitingQueue.peek();
                    if (token == null
                            || batch.getKartCount() + token.getGroupSize() > MAX_TRACK_KARTS) {
                        break;
                    }
                    waitingQueue.poll();
                    if (token.getStatus() == TokenStatus.WAITING) {
                        token.setStatus(TokenStatus.SCHEDULED);
                        batch.addToken(token);
                    }
                }
                if (!batch.getTokens().isEmpty()) {
                    batches.add(batch);
                    newBatches.add(batch);
                    System.out.println("[LOG] Batch scheduled: " + batch.getBatchId());
                }
            }
            return newBatches;
        } finally {
            schedulingLock.unlock();
        }
    }

    public int expireWaitingTokens() {
        int expired = 0;
        LocalDateTime expiryTime = LocalDateTime.now().minus(Duration.ofSeconds(tokenExpirySeconds));
        for (BookingToken token : waitingQueue) {
            if (token.getStatus() == TokenStatus.WAITING
                    && token.getCreatedAt().isBefore(expiryTime)
                    && waitingQueue.remove(token)) {
                token.setStatus(TokenStatus.EXPIRED);
                expired++;
                notifications.notify("Token expired: " + token.getTokenId());
            }
        }
        return expired;
    }

    public synchronized RaceBatch startNextRace() {
        if (activeRace.get() != null) {
            throw new RaceStartException("A race is already active");
        }
        schedulingLock.lock();
        try {
            for (RaceBatch batch : batches) {
                if (batch.getStatus() == BatchStatus.SCHEDULED) {
                    if (!activeRace.compareAndSet(null, batch)) {
                        throw new RaceStartException("A race is already active");
                    }
                    kartsInUse.set(batch.getKartCount());
                    batch.markStarted();
                    for (BookingToken token : batch.getTokens()) token.setStatus(TokenStatus.RACING);
                    notifications.notify("Race started: " + batch.getBatchId());
                    return batch;
                }
            }
        } finally {
            schedulingLock.unlock();
        }
        throw new RaceStartException("No scheduled batch available");
    }

    public synchronized RaceHistory endActiveRace() {
        RaceBatch completed = activeRace.getAndSet(null);
        if (completed == null) throw new RaceEndException("No active race is running");
        for (BookingToken token : completed.getTokens()) token.setStatus(TokenStatus.COMPLETED);
        completed.markEnded();
        RaceHistory result = new RaceHistory(completed);
        history.add(result);
        kartsInUse.set(0);
        notifications.notify("Race ended: " + completed.getBatchId());
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
        if (token.getStatus() == TokenStatus.COMPLETED
                || token.getStatus() == TokenStatus.CANCELLED
                || token.getStatus() == TokenStatus.EXPIRED
                || token.getStatus() == TokenStatus.RACING) {
            throw new InvalidTokenStateException("This token cannot be cancelled now");
        }
        waitingQueue.remove(token);
        for (RaceBatch batch : batches) {
            if (batch.getStatus() == BatchStatus.SCHEDULED) batch.removeToken(token);
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
        waitingQueue.add(token);
    }

    public BookingToken getToken(String tokenId) {
        BookingToken token = tokens.get(tokenId);
        if (token == null) throw new TokenNotFoundException("Token not found: " + tokenId);
        return token;
    }

    public List<BookingToken> getWaitingTokens() {
        return new ArrayList<BookingToken>(waitingQueue);
    }

    public List<RaceBatch> getScheduledBatches() {
        List<RaceBatch> result = new ArrayList<RaceBatch>();
        synchronized (batches) {
            for (RaceBatch batch : batches) {
                if (batch.getStatus() == BatchStatus.SCHEDULED) result.add(batch);
            }
        }
        return result;
    }

    public List<RaceBatch> getAllBatches() {
        synchronized (batches) {
            return new ArrayList<RaceBatch>(batches);
        }
    }

    public List<RaceHistory> getHistory() {
        synchronized (history) {
            return new ArrayList<RaceHistory>(history);
        }
    }

    public RaceBatch getActiveRace() { return activeRace.get(); }
    public int getKartsInUse() { return kartsInUse.get(); }

    public double getTotalRevenue() {
        double total = 0;
        synchronized (history) {
            for (RaceHistory item : history) total += item.getRevenue();
        }
        return total;
    }
}
