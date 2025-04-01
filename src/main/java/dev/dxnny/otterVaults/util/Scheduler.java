package dev.dxnny.otterVaults.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class Scheduler {
    private final BukkitScheduler scheduler;
    private final Plugin plugin;

    public Scheduler(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    /**
     * Executes the given {@code Runnable} synchronously on the server's main thread.
     * If the current thread is the server's primary thread, the task is executed immediately.
     * Otherwise, it is scheduled to run using the server's scheduler.
     *
     * @param runnable the {@code Runnable} task to be executed; must not be null
     */
    public void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            scheduler.runTask(plugin, runnable);
        }
    }

    /**
     * Executes the provided {@code Runnable} asynchronously using the server's scheduler.
     *
     * @param runnable the {@code Runnable} task to be executed asynchronously; must not be null
     */
    public void runAsync(Runnable runnable) {
        scheduler.runTaskAsynchronously(plugin, runnable);
    }
}
