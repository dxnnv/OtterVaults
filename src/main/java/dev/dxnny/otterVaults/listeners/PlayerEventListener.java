package dev.dxnny.otterVaults.listeners;

import dev.dxnny.otterVaults.config.Options;
import dev.dxnny.otterVaults.lang.Messages;
import dev.dxnny.otterVaults.managers.Vault;
import dev.dxnny.otterVaults.managers.VaultManager;
import dev.dxnny.otterVaults.util.Permissions;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static dev.dxnny.otterVaults.OtterVaults.DATABASE;
import static dev.dxnny.otterVaults.OtterVaults.debug;
import static dev.dxnny.otterVaults.lang.Messages.mmSend;
import static dev.dxnny.otterVaults.util.Permissions.hasPerm;

public class PlayerEventListener implements Listener {
    private final VaultManager vaultManager;

    public PlayerEventListener(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        Vault vault = vaultManager.getActiveVaults().values().stream()
                .filter(loopVault -> loopVault.getInventory().equals(event.getInventory()))
                .findFirst()
                .orElse(null);

        if (vault != null) {
            vaultManager.deactivateVault(vault);
            DATABASE().saveVaultAsync(vault);
            vault.unlock();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!Options.isITEM_BLACKLIST_ENABLED()) return;

        debug("blacklist enabled");
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean vaultEvent = false;

        for (Vault vault : vaultManager.getActiveVaultsForPlayer(player.getUniqueId())) {
            InventoryHolder eventHolder = event.getInventory().getHolder();
            InventoryHolder vaultHolder = vault.getInventory().getHolder();
            debug("event holder: " + eventHolder);
            debug("vault holder: " + vaultHolder);

            if (eventHolder == vaultHolder || eventHolder == player) {
                vaultEvent = true;
                debug("vault event");
                break;
            }
        }
        if (!vaultEvent) return;

        if (hasPerm(player, Permissions.BYPASS_BLACKLIST)) return;

        final ItemStack itemStack = event.getClick() == ClickType.NUMBER_KEY ? player.getInventory().getItem(event.getHotbarButton()) : event.getCurrentItem();
        if (itemStack != null) handleBlacklistedItem(player, event, itemStack);

    }

    public void handleBlacklistedItem(Player player, InventoryClickEvent event, ItemStack itemStack) {
        if (isBlacklistedItem(itemStack)) {
            debug("blacklisted item");
            event.setCancelled(true);
            mmSend(player, Messages.ERROR_BLACKLISTED_ITEM);
        }
    }

    private boolean isBlacklistedItem(ItemStack itemStack) {
        List<String> materialBlacklist = Options.getITEM_BLACKLIST_MATERIALS();
        List<String> enchantBlacklist = Options.getITEM_BLACKLIST_ENCHANTS();

        if (materialBlacklist.contains(itemStack.getType().toString())) {
            return true;
        }

        if (!enchantBlacklist.isEmpty()){
            Registry<Enchantment> enchantRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
            for (String enchant : enchantBlacklist) {
                debug("checking blacklist enchantment: " + enchant);
                Enchantment enchantKey = enchantRegistry.get(NamespacedKey.minecraft(enchant.toLowerCase()));

                if (enchantKey == null) continue;
                if (itemStack.containsEnchantment(enchantKey)) return true;
            }
        }

        return false;
    }

}