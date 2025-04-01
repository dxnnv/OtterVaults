package dev.dxnny.otterVaults.storage;

import dev.dxnny.otterVaults.OtterVaults;
import dev.dxnny.otterVaults.managers.Vault;
import dev.dxnny.otterVaults.managers.VaultHolder;
import dev.dxnny.otterVaults.util.Serializers;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.dxnny.otterVaults.lang.Messages.vaultTitle;

public class VaultDatabase {
    private final HikariDataSource dataSource;
    private final Logger logger;

    public VaultDatabase(OtterVaults plugin) {
        this.logger = plugin.getLogger();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:./" + plugin.getDataFolder() + "/data;AUTO_SERVER=TRUE");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(20); // Adjust based on expected load
        config.setConnectionTimeout(15000);
        config.setAutoCommit(true);

        this.dataSource = new HikariDataSource(config);
        initialize();
    }

    /**
     * Initializes the database for storing vaults.
     * The table structure includes:
     * `player_uuid`: A string of the player's UUID,
     * `vault_number`: An integer representing the vault index, and
     * `data`: A BLOB storing the serialized vault data.
     * A key comprised of `player_uuid` and `vault_number` serves as the primary
     * key to identify each vault.
     */
    private void initialize() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS vaults (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "vault_number INT NOT NULL, " +
                    "data BLOB, " +
                    "PRIMARY KEY (player_uuid, vault_number)" +
                    ")"
            );
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error while setting up database connection!", e);
        }
    }

    /**
     * Loads a specific vault identified by the player's UUID and vault number from the database.
     * The method retrieves the serialized vault data from the database, deserializes it into
     * an inventory object, and then returns a Vault instance containing the deserialized inventory.
     * If the vault is not found or an SQL error occurs, the method returns null.
     *
     * @param playerUUID the UUID of the player whose vault is being loaded
     * @param vaultNumber the number identifying the specific vault to load
     * @return a Vault object containing the deserialized inventory if found, or null if the vault is not found or an error occurs
     */
    public Vault loadVault(UUID playerUUID, int vaultNumber) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT data FROM vaults WHERE player_uuid = ? AND vault_number = ?")) {
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, vaultNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] blob = rs.getBytes("data");
                    ItemStack[] contents = Serializers.deserializeInv(blob);

                    Inventory inv = Bukkit.createInventory(Bukkit.getPlayer(playerUUID), 54, vaultTitle(vaultNumber));
                    if (contents != null) {
                        inv.setContents(contents);
                    }

                    return new Vault(playerUUID, vaultNumber, inv);
                }
            }
        } catch (SQLException e) {
            logger.warning("Error while loading vault #" + vaultNumber + " for player " + playerUUID + ": " + e.getMessage());
        }
        return null; // Vault not found
    }

    /**
     * Loads a VaultHolder object for a specific player identified by their UUID.
     * The method retrieves all vaults associated with the player from the database,
     * deserializes their data into inventories, and populates a VaultHolder with
     * the Vault objects.
     *
     * @param playerUUID the UUID of the player whose VaultHolder is to be loaded
     * @return a VaultHolder instance containing the player's vaults, or an empty
     *         VaultHolder if no vaults are found or an error occurs
     */
    public VaultHolder loadVaultHolder(UUID playerUUID) {
        VaultHolder holder = new VaultHolder(playerUUID);
        String sql = "SELECT vault_number, data FROM vaults WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int vaultNumber = rs.getInt("vault_number");
                    byte[] blob = rs.getBytes("data");
                    ItemStack[] contents = Serializers.deserializeInv(blob);

                    Inventory inv = Bukkit.createInventory(Bukkit.getPlayer(playerUUID), 54, vaultTitle(vaultNumber));
                    if (contents != null) {
                        inv.setContents(contents);
                    }

                    Vault vault = new Vault(playerUUID, vaultNumber, inv);
                    holder.addVault(vaultNumber, vault);
                }
            }
        } catch (SQLException e) {
            logger.warning("Error while loading vault holder " + holder.getOwnerUUID() + ": " + e.getMessage());
        }
        return holder;
    }

    /**
     * Saves the specified vault to the database synchronously, by serializing the
     * inventory of the given {@code Vault} object and stores it in the database.
     * Logs a warning to console if the operation takes longer than 100ms.
     * Should be called asynchronously.
     *
     * @param vault the {@code Vault} instance to be saved to the database
     */
    public void saveVault(Vault vault) {
        long startTime = System.nanoTime();
        byte[] data = Serializers.serializeInv(vault.getInventory().getContents());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("MERGE INTO vaults (player_uuid, vault_number, data) KEY (player_uuid, vault_number) VALUES (?, ?, ?)")) {
            ps.setString(1, vault.getOwnerUUID().toString());
            ps.setInt(2, vault.getVaultNumber());
            ps.setBytes(3, data);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error while saving vault " + vault.getVaultNumber() + " for " + vault.getOwnerUUID().toString(), e);
        } finally {
            long duration = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
            if (durationMs > 100) {
                logger.warning("Warning: Took " + durationMs + "ms to save vault " + vault.getVaultNumber());
            }
        }
    }

    /**
     * Asynchronously saves the given vault to the database,
     * to prevent blocking the main server thread.
     *
     * @param vault the {@code Vault} instance to be saved to the database
     */
    public void saveVaultAsync(Vault vault) {
        OtterVaults.getTaskLimiter().submitTask(() -> saveVault(vault));
    }

    public void disable() {
        dataSource.close();
    }
}
