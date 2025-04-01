package dev.dxnny.otterVaults.managers;

import dev.dxnny.otterVaults.storage.VaultDatabase;
import lombok.Getter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class VaultManager {

    @Getter
    private final ConcurrentHashMap<UUID, VaultHolder> holders = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, Vault> activeVaults = new ConcurrentHashMap<>();
    // Offline players' vault holders are cached for 10 minutes after last usage.
    private final Cache<UUID, VaultHolder> vaultHolderCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private final VaultDatabase database;

    public VaultManager(VaultDatabase database) {
        this.database = database;
    }

    /**
     * Generates a unique key for a vault based on the owner's UUID and the vault number.
     *
     * @param uuid the UUID of the vault owner
     * @param vaultNumber the number identifying the specific vault
     * @return a unique string key representing the vault
     */
    public String vaultKey(UUID uuid, int vaultNumber) {
        return uuid.toString() + "-" + vaultNumber;
    }

    /**
     * Retrieves a Vault associated with the specified owner UUID and vault number.
     * If the vault is not already loaded in memory, it attempts to load it from the database.
     * If the vault does not exist in the database, a new vault is created and initialized.
     *
     * @param uuid the UUID of the vault owner
     * @param vaultNumber the number identifying the specific vault
     * @return the Vault instance associated with the specified UUID and vault number
     */
    public Vault getVault(UUID uuid, int vaultNumber) {
        VaultHolder holder = holders.computeIfAbsent(uuid, VaultHolder::new);
        Vault vault = holder.getVault(vaultNumber);

        if (vault == null) {
            // Try loading from database.
            vault = database.loadVault(uuid, vaultNumber);
            if (vault == null) {
                // Create a new vault if it does not already exist.
                vault = new Vault(uuid, vaultNumber);
            }
            holder.addVault(vaultNumber, vault);
        }
        return vault;
    }

    /**
     * Unloads the specified vault and saves it to the database if necessary.
     * If the vault was initially empty and remains unchanged, it is not saved.
     *
     * @param vault the vault to be unloaded
     */
    public void unloadVault(Vault vault) {
        // If the vault was originally empty and remains empty, skip saving as it hasn't changed.
        if (vault.isInitiallyEmpty() && vault.isEmpty())
            return;
        database.saveVaultAsync(vault);
    }

    /**
     * Retrieves a {@code VaultHolder} associated with the specified UUID.
     * If not present in the cache, it will attempt to load it synchronously
     * from the database. If successful, it will be cached for future retrievals.
     *
     * @param uuid the UUID of the owner associated with the {@code VaultHolder}
     * @return the {@code VaultHolder} instance associated with the provided UUID,
     *         or {@code null} if no {@code VaultHolder} exists for the specified UUID.
     */
    public VaultHolder getVaultHolder(UUID uuid) {
        if (holders.containsKey(uuid)) {
            return holders.get(uuid);
        }

        VaultHolder holder = vaultHolderCache.getIfPresent(uuid);
        if (holder == null) {
            // Synchronously load the VaultHolder from the database.
            holder = database.loadVaultHolder(uuid);
            if (holder != null) {
                holders.put(uuid, holder);
            }
        }
        return holder;
    }

    /**
     * Unloads the {@code VaultHolder} associated with the specified UUID,
     * and saves each vault if necessary. Adds the {@code VaultHolder} to
     * the cache in case the vault is re-accessed in the next 10 minutes.
     *
     * @param uuid the UUID of the owner associated with the {@code VaultHolder} to be unloaded
     */
    public void unloadVaultHolder(UUID uuid) {
        VaultHolder holder = holders.remove(uuid);
        if (holder != null) {
            vaultHolderCache.put(uuid, holder);
            for (Vault vault : holder.getVaults().values()) {
                unloadVault(vault);
            }
        }
    }

    /**
     * Retrieves a list of vault numbers from the {@code VaultHolder}
     * associated with the specified UUID.
     *
     * @param uuid the UUID of the owner to retrieve vaults from
     * @return a list of integers representing the vault numbers associated
     *         with the specified UUID, or an empty list if no vaults are found
     */
    public List<Integer> getVaultNumbers(UUID uuid) {
        VaultHolder holder = getVaultHolder(uuid);
        if (holder != null) {
            return holder.getVaultNumbers();
        }
        return Collections.emptyList();
    }

    /**
     * Activates the specified vault by adding it to the collection of active vaults.
     *
     * @param vault the vault to be activated
     */
    public void activateVault(Vault vault) {
        activeVaults.put(vaultKey(vault.getOwnerUUID(), vault.getVaultNumber()), vault);
    }

    /**
     * Deactivates the specified vault by removing its key from the collection of active vaults.
     *
     * @param vault the vault to be deactivated
     */
    public void deactivateVault(Vault vault) {
        activeVaults.remove(vaultKey(vault.getOwnerUUID(), vault.getVaultNumber()));
    }

    /**
     * Retrieves a list of active vaults owned by the specified player.
     *
     * @param playerUUID the UUID of the player whose active vaults should be retrieved
     * @return a list of {@code Vault} instances that are currently active and owned by the given player
     */
    public List<Vault> getActiveVaultsForPlayer(UUID playerUUID) {
        List<Vault> active = new ArrayList<>();
        for (Vault vault : activeVaults.values()) {
            if (vault.getOwnerUUID().equals(playerUUID)) {
                active.add(vault);
            }
        }
        return active;
    }

    /**
     * Checks if the specified vault is currently open.
     *
     * @param uuid the UUID of the vault owner
     * @param vaultNumber the number identifying the specific vault
     * @return {@code true} if the vault is open, {@code false} otherwise
     */
    public boolean isVaultOpen(UUID uuid, int vaultNumber) {
        return activeVaults.containsKey(vaultKey(uuid, vaultNumber));
    }

    /**
     * Unlocks the specified vault, removes it from the collection of active vaults,
     * and saves changes to the vault asynchronously.
     *
     * @param vault the vault to be unlocked and updated in the system
     */
    public void unlockVault(Vault vault) {
        vault.unlock();
        activeVaults.remove(vaultKey(vault.getOwnerUUID(), vault.getVaultNumber()));

        database.saveVaultAsync(vault);
    }

    /**
     * Saves all vaults currently stored in memory to the database if necessary.
     * This method iterates through all loaded VaultHolders and their associated
     * Vaults. Each vault is saved to the database if it has been modified.
     * The operation must be performed synchronously, ensuring consistent data
     * persistence during shutdown or other critical operations.
     */
    public void flushAllVaults() {
        for (VaultHolder holder : holders.values()) {
            for (Vault vault : holder.getVaults().values()) {
                if (!vault.isInitiallyEmpty() || !vault.isEmpty()) {
                    // Perform a synchronous save on shutdown.
                    database.saveVault(vault);
                }
            }
        }
    }

}