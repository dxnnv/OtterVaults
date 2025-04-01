package dev.dxnny.otterVaults.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;

import static dev.dxnny.otterVaults.OtterVaults.INSTANCE;

@SuppressWarnings("deprecation")
public class Serializers {

    /**
     * Serializes an array of {@code ItemStack} objects into a byte array.
     *
     * @param itemStacks the array of {@code ItemStack} objects to be serialized; must not be null
     * @return a byte array representing the serialized {@code ItemStack} array;
     *         returns {@code null} if an exception occurs during serialization
     */
    public static byte[] serializeInv(ItemStack[] itemStacks) {

        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitOutput = new BukkitObjectOutputStream(byteOutput);

            bukkitOutput.writeObject(itemStacks);
            bukkitOutput.flush();
            bukkitOutput.close();

            return byteOutput.toByteArray();
        } catch (Exception e) {
            INSTANCE().getLogger().log(Level.SEVERE, "Error while serializing inventory!", e);
        }

        return null;
    }

    /**
     * Deserializes an array of {@code ItemStack} objects from the provided {@code InputStream}.
     *
     * @param input the {@code InputStream} containing the serialized {@code ItemStack} data;
     *              must not be null
     * @return an array of {@code ItemStack} objects if deserialization is successful;
     *         returns {@code null} if an exception occurs during deserialization
     */
    public static ItemStack[] deserializeInv(byte[] input) {

        ByteArrayInputStream byteInput = new ByteArrayInputStream(input);

        try(BukkitObjectInputStream bukkitInput = new BukkitObjectInputStream(byteInput)) {
            return (ItemStack[]) bukkitInput.readObject();
        } catch (Exception e) {
            INSTANCE().getLogger().log(Level.SEVERE, "Error while deserializing inventory!", e);
        }

        return null;
    }

}
