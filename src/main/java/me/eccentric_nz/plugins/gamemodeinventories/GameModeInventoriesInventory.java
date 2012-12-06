/*
 * Kristian S. Stangeland aadnk
 * Norway
 * kristian@comphenix.net
 * http://www.comphenix.net/
 */
package me.eccentric_nz.plugins.gamemodeinventories;

import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.v1_4_5.NBTBase;
import net.minecraft.server.v1_4_5.NBTTagCompound;
import net.minecraft.server.v1_4_5.NBTTagList;
import org.apache.commons.lang.StringUtils;
import org.bukkit.craftbukkit.v1_4_5.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_4_5.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GameModeInventoriesInventory {

    private final static String textBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private final static char[] alphabetBase64 = textBase64.toCharArray();
    private final static String regexBase64 = "[^" + textBase64 + "=]";

    public static String toBase64(Inventory inventory) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        NBTTagList itemList = new NBTTagList();
        // Save every element in the list
        for (int i = 0; i < inventory.getSize(); i++) {
            NBTTagCompound outputObject = new NBTTagCompound();
            CraftItemStack craft = getCraftVersion(inventory.getItem(i));
            // Convert the item stack to a NBT compound
            if (craft != null) {
                craft.getHandle().save(outputObject);
            }
            itemList.add(outputObject);
        }
        // Now save the list
        NBTBase.a(itemList, dataOutput);
        // Serialize that array
        return new BigInteger(1, outputStream.toByteArray()).toString(32);
        //return encodeBase64(outputStream.toByteArray());
    }

    public static Inventory fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(data, 32).toByteArray());
        //ByteArrayInputStream inputStream = new ByteArrayInputStream(decodeBase64(data));
        NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
        Inventory inventory = new CraftInventoryCustom(null, itemList.size());
        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
            // IsEmpty
            if (!inputObject.d()) {
                inventory.setItem(i, new CraftItemStack(net.minecraft.server.v1_4_5.ItemStack.a(inputObject)));
            }
        }
        // Serialize that array
        return inventory;
    }

    private static String encodeBase64(byte[] data) {
        StringBuilder result = new StringBuilder();
        int padding = data.length % 3;
        int readCount = 0;
        if (padding != 0) {
            padding = 3 - padding;
            data = Bytes.concat(data, new byte[padding]);
        }
        for (int i = 0; i < data.length; i += 3) {
            // Extra lines
            if (i > 0 && readCount % 76 == 0) {
                result.append("\r\n");
            }
            // Read 24 bits
            int window = (data[i] << 16) + (data[i + 1] << 8) + (data[i + 2]);
            // Print 4 chunks of 6 bits (2^6 = 64)
            int n1 = (window >> 18) & 0x3F, n2 = (window >> 12) & 0x3F, n3 = (window >> 6) & 0x3F, n4 = window & 0x3F;
            result.append(alphabetBase64[n1]);
            result.append(alphabetBase64[n2]);
            result.append(alphabetBase64[n3]);
            result.append(alphabetBase64[n4]);
            readCount += 4;
        }

        return result.substring(0, result.length() - padding) + StringUtils.repeat("=", padding);
    }

    private static byte[] decodeBase64(String data) {

        String subset = data.replaceAll(regexBase64, "");
        byte[] result = new byte[3 * subset.length() / 4];
        int count = 0;
        Map<Character, Integer> lookup = new HashMap<Character, Integer>();
        // Initialize lookup table
        for (int i = 0; i < alphabetBase64.length; i++) {
            lookup.put(alphabetBase64[i], i);
        }
        lookup.put('=', 0);
        // Process four characters at a time
        for (int i = 0; i < subset.length(); i += 4) {
            // Convert these characters into the previous 24 bit number
            int n = (lookup.get(subset.charAt(i)) << 18) | (lookup.get(subset.charAt(i + 1)) << 12)
                    | (lookup.get(subset.charAt(i + 2)) << 6) | lookup.get(subset.charAt(i + 3));

            // Get the bytes it consists of
            result[count] = (byte) ((n >>> 16) & 0xFF);
            result[count + 1] = (byte) ((n >>> 8) & 0xFF);
            result[count + 2] = (byte) (n & 0xFF);
            count += 3;
        }
        return result;
    }

    private static CraftItemStack getCraftVersion(ItemStack stack) {
        if (stack instanceof CraftItemStack) {
            return (CraftItemStack) stack;
        } else if (stack != null) {
            return new CraftItemStack(stack);
        } else {
            return null;
        }
    }
}