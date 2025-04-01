package dev.dxnny.otterVaults.util;

import dev.dxnny.otterVaults.config.Options;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class ArgumentParser {

    @Getter
    public static class VaultCommandArguments {
        private final UUID targetUUID;
        private final int vaultNumber;

        public VaultCommandArguments(UUID targetUUID, int vaultNumber) {
            this.targetUUID = targetUUID;
            this.vaultNumber = vaultNumber;
        }

    }

    public static VaultCommandArguments parseArguments(String target, String vault, UUID senderUUID) throws IllegalArgumentException {
        UUID targetUUID = null;
        int vaultNumber;

        // Determine if target is a player or vault number
        if (target == null) {
            targetUUID = senderUUID;
        } else {
            // Check if target is numeric
            try {
                vaultNumber = Integer.parseInt(target);
                if (vaultNumber <= Options.getMAX_VAULT_COUNT())targetUUID = senderUUID;
                return new VaultCommandArguments(targetUUID, vaultNumber);
            } catch (NumberFormatException ignored) {
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(target);
                if (targetPlayer.hasPlayedBefore() || targetPlayer.isOnline()) {
                    targetUUID = targetPlayer.getUniqueId();
                }
            }
        }

        if (targetUUID == null) {
            throw new IllegalArgumentException("player");
        }

        try {
            vaultNumber = Integer.parseInt(vault);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("number");
        }

        return new VaultCommandArguments(targetUUID, vaultNumber);
    }
}