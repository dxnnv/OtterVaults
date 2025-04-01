package dev.dxnny.otterVaults;

import co.aikar.commands.PaperCommandManager;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dxnny.otterVaults.config.Options;
import dev.dxnny.otterVaults.commands.VaultCommand;
import dev.dxnny.otterVaults.lang.Messages;
import dev.dxnny.otterVaults.listeners.PlayerEventListener;
import dev.dxnny.otterVaults.listeners.PlayerInstanceListener;
import dev.dxnny.otterVaults.managers.VaultManager;
import dev.dxnny.otterVaults.storage.TaskLimiter;
import dev.dxnny.otterVaults.storage.VaultDatabase;
import dev.dxnny.otterVaults.util.Scheduler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public final class OtterVaults extends JavaPlugin {
    private YamlDocument config;
    private static OtterVaults instance;
    @Getter
    private static VaultManager vaultManager;
    @Getter
    private static TaskLimiter taskLimiter;
    @Getter
    private static Scheduler scheduler;
    private static VaultDatabase database;
    private static boolean DEBUG;

    @Override
    public void onEnable() {
        instance = this;
        taskLimiter = new TaskLimiter(15);
        scheduler = new Scheduler(this);

        loadConfig();
        DEBUG = Options.isDEBUG();
        Messages.initialize(this);

        debug("Initializing database...");
        database = new VaultDatabase(this);
        vaultManager = new VaultManager(database);

        // Register events
        debug("Registering listeners...");
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new PlayerEventListener(vaultManager), this);
        manager.registerEvents(new PlayerInstanceListener(vaultManager, database), this);

        // Register commands
        debug("Registering commands...");
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new VaultCommand(vaultManager));

    }

    @Override
    public void onDisable() {

        // Shutdown properly, flushing all vaults
        // synchronously to ensure data is saved.
        debug("Saving vaults...");
        vaultManager.flushAllVaults();
        debug("Shutting down all tasks...");
        Bukkit.getScheduler().cancelTasks(this);
        if (taskLimiter != null) {
            taskLimiter.shutdown();
        }
        debug("Shutdown complete!");

    }

    private void loadConfig() {
        try {
            config = YamlDocument.create(
                    new File(this.getDataFolder() + "/config.yml"),
                    Objects.requireNonNull(getResource("config.yml")),
                    DumperSettings.builder().setIndentation(4).build()
            );
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while loading the config!", e);
        }
        Options.loadOptions(config);
    }

    /**
     * Logs the message provided if debug mode is enabled.
     *
     * @param msg the message to be logged; must not be null or empty
     */
    public static void debug(String msg) {
        if (DEBUG) instance.getLogger().info(msg);
    }

    /**
     * Returns the singleton instance of the plugin.
     *
     * @return The singleton instance of the {@code OtterVaults} plugin.
     */
    public static Plugin INSTANCE() {
        return instance;
    }

    /**
     * Provides access to the singleton instance of the {@code VaultDatabase}.
     *
     * @return The singleton instance of {@code VaultDatabase}, initialized and ready for use.
     */
    public static VaultDatabase DATABASE() {
        return database;
    }

}
