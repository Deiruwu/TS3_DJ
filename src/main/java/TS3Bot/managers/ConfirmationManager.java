package TS3Bot.managers;

import java.util.Map;
import java.util.concurrent.*;

public class ConfirmationManager {
    private final Map<String, PendingConfirmation> pendingConfirmations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void requestConfirmation(String userUid, int clientId,
                                    Runnable onConfirm, Runnable onCancel, int timeoutSeconds) {

        cancelPending(userUid);

        PendingConfirmation pending = new PendingConfirmation(userUid, clientId, onConfirm, onCancel);
        pendingConfirmations.put(userUid, pending);

        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            if (pendingConfirmations.remove(userUid) != null) {
                onCancel.run();
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        pending.setTimeoutTask(timeoutTask);
    }

    public boolean handleResponse(String userUid, String response) {
        PendingConfirmation pending = pendingConfirmations.remove(userUid);
        if (pending == null) return false;

        pending.cancelTimeout();

        if (response.trim().equalsIgnoreCase("y")) {
            pending.getOnConfirm().run();
            return true;
        } else {
            pending.getOnCancel().run();
            return true;
        }
    }

    public void cancelPending(String userUid) {
        PendingConfirmation pending = pendingConfirmations.remove(userUid);
        if (pending != null) {
            pending.cancelTimeout();
            pending.getOnCancel().run();
        }
    }

    public boolean hasPending(String userUid) {
        return pendingConfirmations.containsKey(userUid);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private static class PendingConfirmation {
        private final String userUid;
        private final int clientId;
        private final Runnable onConfirm;
        private final Runnable onCancel;
        private ScheduledFuture<?> timeoutTask;

        public PendingConfirmation(String userUid, int clientId, Runnable onConfirm, Runnable onCancel) {
            this.userUid = userUid;
            this.clientId = clientId;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
        }

        public void setTimeoutTask(ScheduledFuture<?> timeoutTask) {
            this.timeoutTask = timeoutTask;
        }

        public void cancelTimeout() {
            if (timeoutTask != null && !timeoutTask.isDone()) {
                timeoutTask.cancel(false);
            }
        }

        public Runnable getOnConfirm() { return onConfirm; }
        public Runnable getOnCancel() { return onCancel; }
        public int getClientId() { return clientId; }
    }
}