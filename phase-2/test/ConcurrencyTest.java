import enums.KartType;
import models.BookingToken;
import models.Customer;
import models.RaceBatch;
import services.ArenaService;

public class ConcurrencyTest {
    public static void main(String[] args) throws Exception {
        ArenaService arena = new ArenaService(60, 30);
        Customer[] customers = new Customer[5];
        for (int i = 0; i < customers.length; i++) {
            customers[i] = arena.registerCustomer("test" + i, "test", "Test " + i);
        }

        Thread[] bookingThreads = new Thread[customers.length];
        for (int i = 0; i < customers.length; i++) {
            final Customer customer = customers[i];
            bookingThreads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    arena.book(customer, 2, 5, KartType.SINGLE);
                }
            });
            bookingThreads[i].start();
        }
        for (Thread thread : bookingThreads) thread.join();

        if (arena.getWaitingTokens().size() != 5) {
            throw new AssertionError("Expected five waiting tokens");
        }

        BookingToken cancelled = arena.getWaitingTokens().get(0);
        arena.startBackgroundScheduler();
        arena.cancelToken(cancelled.getTokenId());
        Thread.sleep(100);

        RaceBatch race = arena.startNextRace();
        try {
            arena.startNextRace();
            throw new AssertionError("Two races must not run together");
        } catch (RuntimeException expected) {
            // The active race correctly blocks a second race.
        }
        arena.endActiveRace();
        if (arena.getHistory().size() != 1 || arena.getTotalRevenue() <= 0) {
            throw new AssertionError("Race history and revenue are inconsistent");
        }
        arena.shutdown();
        System.out.println("Concurrency test passed for " + race.getBatchId());
    }
}
