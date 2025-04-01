package dev.dxnny.otterVaults.lang;

import dev.dxnny.otterVaults.OtterVaults;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Getter
public enum Messages {
    GENERIC_PREFIX("generic.prefix"),
    GENERIC_DEBUG("generic.debug"),
    GENERIC_HELP("generic.help"),
    GENERIC_HELP_ADMIN("generic.help.admin"),

    ERROR_CONCURRENT_ACCESS("error.concurrent_access"),
    ERROR_PLAYER_ONLY("error.player_only"),
    ERROR_INVALID_PLAYER("error.invalid.player"),
    ERROR_INVALID_NUMBER("error.invalid.number"),
    ERROR_INVALID_VAULT("error.invalid.vault"),
    ERROR_BLACKLISTED_ITEM("error.blacklisted_item"),

    PERMS_INSUFFICIENT("permissions.insufficient"),
    PERMS_PV_COUNT("permissions.pv_count"),

    COMMANDS_VAULT_LIST("commands.vault.list"),
    COMMANDS_VAULT_OPENING_SELF("commands.vault.opening.self"),
    COMMANDS_VAULT_OPENING_OTHER("commands.vault.opening.other");

    private final String path;

    Messages(String path) {
        this.path = path;
    }

    private static final Map<Messages, String> messages = new HashMap<>();
    private static final MiniMessage mmInstance = MiniMessage.miniMessage();

    public static void initialize(@NotNull OtterVaults plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defaultMessages = plugin.getResource("messages.yml");

        if (defaultMessages != null) {
            InputStreamReader reader = new InputStreamReader(defaultMessages, StandardCharsets.UTF_8);
            YamlConfiguration defaultMessagesConfig = YamlConfiguration.loadConfiguration(reader);
            messagesConfig.setDefaults(defaultMessagesConfig);
        }

        for (Messages message : Messages.values()) {
            String messageValue;
            String messagePath = message.getPath();
            String configEntry = messagesConfig.getString(messagePath);

            if (configEntry != null) {
                messageValue = configEntry;
            } else {
                plugin.getLogger().warning("Missing message for " + messagePath);
                messageValue = messagesConfig.getDefaults() != null ? messagesConfig.getDefaults().getString(messagePath, "null") : "null";
            }

            messages.put(message, messageValue);
        }
    }

    /**
     * Retrieves the value associated with the specified message.
     *
     * @param message the message key for which the value is to be retrieved; must not be null
     * @return the value associated with the given message key
     */
    static String getValue(@NotNull Messages message) {
        return messages.get(message);
    }

    /**
     * Sends a parsed message to a {@link Player} using MiniMessage.
     *
     * @param player    The player to whom the message will be sent.
     * @param path      The {@link Messages} enum constant representing the message that will be sent.
     * @param resolvers Optional {@link TagResolver} parameters to inject dynamic content into the message.
     */
    public static void mmSend(@NotNull Player player, @NotNull Messages path, @Nullable TagResolver... resolvers) {
        player.sendMessage(parsedMessage(path, resolvers));
    }

    /**
     * Parses a message from the specified {@link Messages} enum constant and formats it using MiniMessage,
     * with the plugin's prefix. If optional {@link TagResolver} instances are provided, they will be used
     * to dynamically inject content into the message.
     *
     * @param path      The {@link Messages} enum constant representing the message to be parsed.
     * @param resolvers Optional {@link TagResolver} instances for injecting dynamic content into the message.
     * @return A {@link Component} object representing the parsed and formatted message.
     * @throws IllegalStateException If the message corresponding to the provided {@link Messages} key is missing.
     */
    public static Component parsedMessage(@NotNull Messages path, @Nullable TagResolver... resolvers) {
        return resolvers != null
                ? mmInstance.deserialize(getValue(Messages.GENERIC_PREFIX) + getValue(path), resolvers)
                : mmInstance.deserialize(getValue(Messages.GENERIC_PREFIX) + getValue(path));
    }

    /**
     * Generates a vault title component for a given vault number.
     *
     * @param vaultNumber the number of the vault to include in the title
     * @return a Component representing the serialized vault title
     */
    public static Component vaultTitle(int vaultNumber) {
        return mmInstance.deserialize("<dark_gray>Vault " + vaultNumber);
    }

}