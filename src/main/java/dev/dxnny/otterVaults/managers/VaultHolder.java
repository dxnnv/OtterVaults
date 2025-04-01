package dev.dxnny.otterVaults.managers;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class VaultHolder {
    private final UUID ownerUUID;
    private final Map<Integer, Vault> vaults = new ConcurrentHashMap<>();

    /**
     * Constructs a VaultHolder instance for a specified owner.
     *
     * @param ownerUUID the uuid of the owner associated with this VaultHolder
     */
    public VaultHolder(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    /**
     * Adds or replaces a vault in the VaultHolder's collection, associated with the specified vault number.
     *
     * @param vaultNumber the number of the vault
     * @param vault the Vault instance associated with the vault number
     */
    public void addVault(int vaultNumber, Vault vault) {
        vaults.put(vaultNumber, vault);
    }

    /**
     * Retrieves the Vault associated with the specified vault number.
     *
     * @param vaultNumber the number of the vault
     * @return the Vault instance associated with the vault number,
     *         or {@code null} if no vault is associated.
     */
    public Vault getVault(int vaultNumber) {
        return vaults.get(vaultNumber);
    }

    /**
     * Retrieves a list of all vault numbers managed by this VaultHolder.
     *
     * @return a list of integers representing the vault numbers.
     */
    public List<Integer> getVaultNumbers() {
        return new ArrayList<>(vaults.keySet());
    }

}
