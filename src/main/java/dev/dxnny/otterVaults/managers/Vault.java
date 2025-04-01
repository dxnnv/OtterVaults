package dev.dxnny.otterVaults.managers;

import dev.dxnny.otterVaults.OtterVaults;
import dev.dxnny.otterVaults.lang.Messages;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static dev.dxnny.otterVaults.lang.Messages.mmSend;
import static dev.dxnny.otterVaults.lang.Messages.vaultTitle;


public class Vault {
    @Getter
    private final UUID ownerUUID;
    @Getter
    private final int vaultNumber;
    @Getter
    private Inventory inventory;
    @Getter
    private final boolean initiallyEmpty;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructs a Vault instance with the specified owner, vault number, and inventory.
     *
     * @param ownerUUID the uuid of the owner of the vault
     * @param vaultNumber the number identifying the specific vault
     * @param vaultInventory the inventory associated with the vault
     */
    public Vault(UUID ownerUUID, int vaultNumber, Inventory vaultInventory) {
        this.ownerUUID = ownerUUID;
        this.vaultNumber = vaultNumber;
        this.inventory = vaultInventory;
        this.initiallyEmpty = isInventoryEmpty(vaultInventory);
    }

    /**
     * Constructs a Vault instance with the specified owner and vault number, and an empty inventory.
     *
     * @param ownerUUID the uuid of the owner of the vault
     * @param vaultNumber the number identifying the specific vault
     */
    public Vault(UUID ownerUUID, int vaultNumber) {
        this.ownerUUID = ownerUUID;
        this.vaultNumber = vaultNumber;
        this.inventory = Bukkit.createInventory(Bukkit.getPlayer(ownerUUID), 54, vaultTitle(vaultNumber));
        this.initiallyEmpty = true;
    }

    /**
     * Attempts to acquire the lock for the vault.
     * If the lock is available, it will be acquired by the current thread.
     *
     * @return {@code true} if the lock was successfully acquired, false otherwise.
     */
    public boolean lock() {
        return lock.tryLock();
    }

    /**
     * Releases the lock held on the current vault, if the current thread holds that lock.
     */
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Opens the vault inventory for the specified player.
     *
     * @param player the player to open the inventory for
     */
    public void openInventory(Player player) {
        VaultManager vaultManager = OtterVaults.getVaultManager();
        if (!lock()) {
            mmSend(player, Messages.ERROR_CONCURRENT_ACCESS);
            return;
        }

        vaultManager.activateVault(this);
        OtterVaults.getScheduler().runSync(() -> {
            getInventory().close();
            player.openInventory(inventory);
        });
    }

    /**
     * Checks if the given inventory is empty.
     *
     * @param inv the inventory to check for emptiness
     * @return {@code true} if the inventory is empty, {@code false} otherwise
     */
    private boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if the vault's inventory is empty.
     *
     * @return {@code true} if the inventory is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return isInventoryEmpty(this.inventory);
    }

}