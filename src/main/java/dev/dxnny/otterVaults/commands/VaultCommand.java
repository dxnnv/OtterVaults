package dev.dxnny.otterVaults.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.dxnny.otterVaults.lang.Messages;
import dev.dxnny.otterVaults.managers.Vault;
import dev.dxnny.otterVaults.managers.VaultManager;
import dev.dxnny.otterVaults.util.ArgumentParser;
import dev.dxnny.otterVaults.util.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.dxnny.otterVaults.lang.Messages.*;

@CommandAlias("vaults|ottervaults|playervaults|pv")
public class VaultCommand extends BaseCommand {
    private final VaultManager vaultManager;

    public VaultCommand(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
    }

    @Default
    @CommandCompletion("@players")
    @CommandPermission(Permissions.COMMAND_USE)
    @Description("Open a vault or one of another player. Lists vaults if no number provided")
    @Syntax("[player] [vault]")
    public boolean onCommand(CommandSender sender, @Name("player") @Optional String target, @Default("0") @Name("vault") String vaultString) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(parsedMessage(Messages.ERROR_PLAYER_ONLY));
            return true;
        }

        try {
            ArgumentParser.VaultCommandArguments args = ArgumentParser.parseArguments(target, vaultString, player.getUniqueId());

            UUID targetUUID = args.getTargetUUID();
            int vaultNumber = args.getVaultNumber();

            return handleCommand(player, targetUUID, vaultNumber);

        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("player")) {
                mmSend(player, Messages.ERROR_INVALID_PLAYER);
            } else if (e.getMessage().equals("number")) {
                mmSend(player, Messages.ERROR_INVALID_NUMBER);
            }

            return false;
        }
    }

    private boolean handleCommand(Player player, UUID targetUUID, int vaultNumber) {
        if (vaultNumber <= 0) {
            List<Integer> vaults = vaultManager.getVaultNumbers(targetUUID);
            String vaultList = vaults.isEmpty() ? "None" : vaults.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            mmSend(player, Messages.COMMANDS_VAULT_LIST, Placeholder.parsed("vaults", vaultList));
            return true;
        }

        if (vaultManager.isVaultOpen(targetUUID, vaultNumber)) {
            mmSend(player, Messages.ERROR_CONCURRENT_ACCESS);
            return false;
        }

        Vault vault = vaultManager.getVault(targetUUID, vaultNumber);
        if (vault == null) {
            mmSend(player, Messages.ERROR_INVALID_VAULT);
            return false;
        }

        if (!Permissions.hasVaultCount(player.getUniqueId(), vaultNumber)) {
            mmSend(player, PERMS_PV_COUNT);
            return false;
        }
        vault.openInventory(player);

        if (!targetUUID.equals(player.getUniqueId())) {
            mmSend(player, Messages.COMMANDS_VAULT_OPENING_OTHER,
                    Placeholder.parsed("number", String.valueOf(vaultNumber)),
                    Placeholder.parsed("player", Objects.requireNonNull(Bukkit.getOfflinePlayer(targetUUID).getName())));
        } else {
            mmSend(player, Messages.COMMANDS_VAULT_OPENING_SELF, Placeholder.parsed("number", String.valueOf(vaultNumber)));
        }
        return true;
    }

}