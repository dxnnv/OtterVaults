package dev.dxnny.otterVaults.util;

import dev.dxnny.otterVaults.config.Options;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Permissions {

    public static final String ADMIN = "ottervaults.admin";
    public static final String COMMAND_USE = "ottervaults.use";
    public static final String BYPASS_BLACKLIST = "ottervaults.bypassblacklist";
    private static final String PV_AMOUNT = "ottervaults.amount.";

    /**
     * Checks if the specified player has a given permission (or admin permission).
     *
     * @param offlinePlayer the player whose permissions are being verified; must not be null
     * @param permission the specific permission string to check for
     * @return {@code true} if the player has the specified permission (or admin permission);
     *         {@code false} otherwise
     */
    public static boolean hasPerm(OfflinePlayer offlinePlayer, String permission) {
        Player player = offlinePlayer.getPlayer();
        return player != null && (player.hasPermission(permission) || player.hasPermission(ADMIN));
    }

    /**
     * Determines whether a player identified by their UUID has a specific number of vaults
     * available to them based on their permissions.
     *
     * @param uuid the UUID of the player whose permissions are being checked
     * @param count the number of vaults to check for in the player's permissions
     * @return {@code true} if the player exists and has sufficient permissions for the specified
     *         number of vaults (or admin permission), {@code false} otherwise
     */
    public static boolean hasVaultCount(UUID uuid, Integer count) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (hasPerm(player, ADMIN)) return true;

        for (int i = count; i <= Options.getMAX_VAULT_COUNT(); i++) {
            if (hasPerm(player, PV_AMOUNT + i)) {
                return true;
            }
        }

        return false;
    }

}
