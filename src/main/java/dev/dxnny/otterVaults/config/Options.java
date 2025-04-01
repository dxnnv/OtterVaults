package dev.dxnny.otterVaults.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;

import java.util.List;

public class Options {

    @Getter
    private static int MAX_VAULT_COUNT;
    @Getter
    private static boolean ITEM_BLACKLIST_ENABLED;
    @Getter
    private static List<String> ITEM_BLACKLIST_MATERIALS;
    @Getter
    private static List<String> ITEM_BLACKLIST_ENCHANTS;
    @Getter
    private static boolean DEBUG;

    public static void loadOptions(YamlDocument config) {
        MAX_VAULT_COUNT = config.getInt("max_vault_count", 20);
        ITEM_BLACKLIST_ENABLED = config.getBoolean("item_blacklist.enabled", false);
        ITEM_BLACKLIST_MATERIALS = config.getStringList("item_blacklist.materials", List.of("AIR"));
        ITEM_BLACKLIST_ENCHANTS = config.getStringList("item_blacklist.enchantments", List.of(""));
        DEBUG = config.getBoolean("debug", false);
    }

}
