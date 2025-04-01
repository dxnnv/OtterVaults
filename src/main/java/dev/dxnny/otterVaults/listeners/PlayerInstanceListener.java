package dev.dxnny.otterVaults.listeners;

import dev.dxnny.otterVaults.OtterVaults;
import dev.dxnny.otterVaults.managers.Vault;
import dev.dxnny.otterVaults.managers.VaultHolder;
import dev.dxnny.otterVaults.managers.VaultManager;
import dev.dxnny.otterVaults.storage.VaultDatabase;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerInstanceListener implements Listener {
    private final VaultManager vaultManager;
    private final VaultDatabase database;

    public PlayerInstanceListener(VaultManager vaultManager, VaultDatabase database) {
        this.vaultManager = vaultManager;
        this.database = database;


        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uuid = player.getUniqueId();

            OtterVaults.getTaskLimiter().submitTask(() -> {
                AtomicReference<VaultHolder> holder = new AtomicReference<>();
                OtterVaults.getScheduler().runAsync(() -> holder.set(database.loadVaultHolder(uuid)));

                // If the holder exists, we need to update the cache on the main thread.
                if (holder.get() != null) {
                    OtterVaults.getScheduler().runSync(() -> vaultManager.getHolders().put(uuid, holder.get()));
                }
            });
        });
    }

    /**
     * Loads the player's {@link VaultHolder} upon joining the server.
     *
     * @param event the PlayerJoinEvent triggered when a player joins the server
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Load from the database asynchronously
        OtterVaults.getTaskLimiter().submitTask(() -> {
            AtomicReference<VaultHolder> holder = new AtomicReference<>();
            OtterVaults.getScheduler().runAsync(() -> holder.set(database.loadVaultHolder(uuid)));

            // If the holder exists, we need to update the cache on the main thread.
            if (holder.get() != null) {
                OtterVaults.getScheduler().runSync(() -> vaultManager.getHolders().put(uuid, holder.get()));
            }
        });
    }

    /**
     * Unlocks and unloads all active vaults associated whenever a player leaves
     * the server.
     *
     * @param event the PlayerQuitEvent triggered when a player leaves the server.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        List<Vault> activeVaults = vaultManager.getActiveVaultsForPlayer(uuid);
        activeVaults.forEach(vaultManager::unlockVault);

        vaultManager.unloadVaultHolder(uuid);
    }

}