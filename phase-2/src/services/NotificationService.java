package services;

public class NotificationService {
    public void notify(String message) {
        Thread notificationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("[NOTIFICATION] " + message);
            }
        }, "notification-thread");
        notificationThread.start();
    }
}
