package dev.dxnny.otterVaults.storage;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;

import static dev.dxnny.otterVaults.OtterVaults.DATABASE;
import static dev.dxnny.otterVaults.OtterVaults.INSTANCE;

public class TaskLimiter {
    private final AtomicInteger currentTasks = new AtomicInteger(0);
    private final int maxConcurrentTasks;
    private final ExecutorService executor;

    public TaskLimiter(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.executor = Executors.newFixedThreadPool(maxConcurrentTasks);
    }

    /**
     * Submits a task to be executed by the executor.
     * If the number of concurrent tasks has reached the maximum limit,
     * the task will be scheduled with a slight delay to ensure throttling.
     *
     * @param task The {@code Runnable} task to be executed.
     */
    public void submitTask(Runnable task) {
        if (currentTasks.get() >= maxConcurrentTasks) {
            // Schedule the task with a slight delay if too many tasks are already running.
            Bukkit.getScheduler().runTaskLater(INSTANCE(), () -> submitTask(task), 5L);
        } else {
            currentTasks.incrementAndGet();
            executor.submit(() -> {
                try {
                    task.run();
                } finally {
                    currentTasks.decrementAndGet();
                }
            });
        }
    }

    /**
     * Safely shuts down the associated {@code ExecutorService}, allowing
     * previously submitted tasks to complete execution within a timeout
     * of 10 seconds. If the executor does not shut down in time, it will
     * attempt to forcefully shut down the executor service.
     */
    public void shutdown() {
        DATABASE().disable();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
