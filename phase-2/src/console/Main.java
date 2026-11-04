package console;

import enums.KartType;
import models.BookingToken;
import models.Customer;
import models.RaceBatch;
import models.RaceHistory;
import services.ArenaService;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    private final ArenaService arena = new ArenaService();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        while (true) {
            System.out.println("\n========== FNF KARTING ARENA PHASE 2 ==========");
            System.out.println("1. Register New Customer");
            System.out.println("2. Login as Customer");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            int choice = readInt("Choose an option: ");
            if (choice == 1) register();
            else if (choice == 2) customerLogin();
            else if (choice == 3) adminLogin();
            else if (choice == 4) {
                arena.shutdown();
                return;
            } else {
                System.out.println("Invalid menu option.");
            }
        }
    }

    private void register() {
        try {
            String name = readLine("Name: ");
            String username = readLine("Username: ");
            String password = readLine("Password: ");
            arena.registerCustomer(username, password, name);
            System.out.println("Registration successful.");
        } catch (RuntimeException ex) {
            System.out.println("Registration failed: " + ex.getMessage());
        }
    }

    private void customerLogin() {
        Customer customer = arena.loginCustomer(
                readLine("Username: "), readLine("Password: "));
        if (customer == null) {
            System.out.println("Invalid credentials.");
            return;
        }

        while (true) {
            System.out.println("\n========= CUSTOMER MENU =========");
            System.out.println("1. Book a Race");
            System.out.println("2. View My Bookings");
            System.out.println("3. Cancel a Booking");
            System.out.println("4. Logout");
            int choice = readInt("Choose an option: ");
            try {
                if (choice == 1) book(customer);
                else if (choice == 2) printBookings(customer);
                else if (choice == 3) {
                    arena.cancelToken(readLine("Token ID: "), customer);
                } else if (choice == 4) {
                    return;
                } else {
                    System.out.println("Invalid menu option.");
                }
            } catch (RuntimeException ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
        }
    }

    private void book(Customer customer) {
        int groupSize = readInt("Group size (1-6): ");
        int laps = readInt("Laps (5, 12, 20): ");
        System.out.println("Kart type: 1. SINGLE  2. TWIN  3. KIDS");
        int kartChoice = readInt("Kart type: ");
        if (kartChoice < 1 || kartChoice > 3) {
            System.out.println("Invalid kart type.");
            return;
        }
        BookingToken token = arena.book(customer, groupSize, laps,
                KartType.values()[kartChoice - 1]);
        System.out.println("Booking created: " + token);
    }

    private void adminLogin() {
        if ("admin".equals(readLine("Admin username: "))
                && "admin".equals(readLine("Admin password: "))) {
            adminMenu();
        } else {
            System.out.println("Invalid admin credentials.");
        }
    }

    private void adminMenu() {
        while (true) {
            System.out.println("\n============== ADMIN PANEL ==============");
            System.out.println("1. View Token Queue");
            System.out.println("2. Schedule and View Batches");
            System.out.println("3. Start Next Race");
            System.out.println("4. End Active Race");
            System.out.println("5. Cancel or Reprioritize Token");
            System.out.println("6. View Race History");
            System.out.println("7. View Earnings Summary");
            System.out.println("8. DEV / ADMIN Test Tools");
            System.out.println("9. Logout");
            int choice = readInt("Choose an option: ");
            try {
                if (choice == 1) {
                    for (BookingToken token : arena.getWaitingTokens()) {
                        System.out.println(token);
                    }
                } else if (choice == 2) {
                    for (RaceBatch batch : arena.scheduleWaitingTokens()) {
                        printBatch(batch);
                    }
                } else if (choice == 3) {
                    printBatch(arena.startNextRace());
                } else if (choice == 4) {
                    System.out.println("Completed: "
                            + arena.endActiveRace().getBatchId());
                } else if (choice == 5) {
                    adminTokenAction();
                } else if (choice == 6) {
                    for (RaceHistory item : arena.getHistory()) {
                        System.out.printf("%s | Rs.%.2f | %s%n",
                                item.getBatchId(), item.getRevenue(),
                                item.getCompletedAt());
                    }
                } else if (choice == 7) {
                    System.out.printf("Completed-race revenue: Rs.%.2f%n",
                            arena.getTotalRevenue());
                } else if (choice == 8) {
                    developerTools();
                } else if (choice == 9) {
                    return;
                } else {
                    System.out.println("Invalid menu option.");
                }
            } catch (RuntimeException ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
        }
    }

    private void developerTools() {
        while (true) {
            System.out.println("\n========== DEV / ADMIN TEST TOOLS ==========");
            System.out.println("1. Simulate Multiple Concurrent Bookings");
            System.out.println("2. Start Background Scheduler Thread");
            System.out.println("3. Auto-Start Race Every 30 Seconds");
            System.out.println("4. Enable Token Expiry Timer (5 min)");
            System.out.println("5. Simulate Admin Cancelling During Batch Assignment");
            System.out.println("6. View Live Race Log");
            System.out.println("7. Back to Admin Menu");
            int choice = readInt("Choose an option: ");
            try {
                if (choice == 1) simulateConcurrentBookings();
                else if (choice == 2) arena.startBackgroundScheduler();
                else if (choice == 3) {
                    arena.startBackgroundScheduler();
                    System.out.println("Automatic race start is enabled.");
                } else if (choice == 4) {
                    arena.startBackgroundScheduler();
                    System.out.println("Token expiry watcher is enabled.");
                } else if (choice == 5) cancelDuringScheduling();
                else if (choice == 6) printLiveRaceLog();
                else if (choice == 7) return;
                else System.out.println("Invalid menu option.");
            } catch (RuntimeException ex) {
                System.out.println("Developer tool failed: " + ex.getMessage());
            }
        }
    }

    private void simulateConcurrentBookings() {
        final Customer[] customers = new Customer[5];
        for (int i = 0; i < customers.length; i++) {
            String username = "demo" + i;
            Customer customer = arena.loginCustomer(username, "demo");
            if (customer == null) {
                customer = arena.registerCustomer(username, "demo",
                        "Demo Customer " + i);
            }
            customers[i] = customer;
        }

        Thread[] bookingThreads = new Thread[customers.length];
        for (int i = 0; i < customers.length; i++) {
            final Customer customer = customers[i];
            bookingThreads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    arena.book(customer, 2, 5, KartType.SINGLE);
                }
            }, "customer-booking-" + i);
            bookingThreads[i].start();
        }

        for (Thread bookingThread : bookingThreads) {
            try {
                bookingThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.out.println("Booking simulation was interrupted.");
                return;
            }
        }
        System.out.println("Five booking threads completed.");
    }

    private void cancelDuringScheduling() {
        arena.startBackgroundScheduler();
        if (arena.getWaitingTokens().isEmpty()) {
            System.out.println("Create a booking before running this test.");
            return;
        }
        String tokenId = arena.getWaitingTokens().get(0).getTokenId();
        arena.cancelToken(tokenId);
        System.out.println("Cancelled " + tokenId
                + " while the scheduler was running.");
    }

    private void printLiveRaceLog() {
        RaceBatch activeRace = arena.getActiveRace();
        System.out.println("Active race: "
                + (activeRace == null ? "none" : activeRace.getBatchId()));
        System.out.println("Karts currently in use: " + arena.getKartsInUse());
        for (RaceBatch batch : arena.getAllBatches()) {
            System.out.println(batch.getBatchId() + " | " + batch.getStatus());
        }
    }

    private void adminTokenAction() {
        String tokenId = readLine("Token ID: ");
        int choice = readInt("1. Cancel  2. Reprioritize: ");
        if (choice == 1) arena.cancelToken(tokenId);
        else if (choice == 2) arena.reprioritizeToken(tokenId);
        else System.out.println("Invalid action.");
    }

    private void printBatch(RaceBatch batch) {
        System.out.println(batch.getBatchId() + " | " + batch.getStatus()
                + " | " + batch.getKartCount() + "/"
                + ArenaService.MAX_TRACK_KARTS + " karts");
        for (BookingToken token : batch.getTokens()) {
            System.out.println("  " + token);
        }
    }

    private void printBookings(Customer customer) {
        for (BookingToken token : customer.getBookings()) {
            System.out.println(token);
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = scanner.nextInt();
                scanner.nextLine();
                return value;
            } catch (InputMismatchException ex) {
                scanner.nextLine();
                System.out.println("Please enter a number.");
            }
        }
    }
}
