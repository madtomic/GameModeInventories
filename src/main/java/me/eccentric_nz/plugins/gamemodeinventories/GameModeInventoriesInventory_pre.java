package me.eccentric_nz.plugins.gamemodeinventories;

import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import org.apache.commons.lang.StringUtils;
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GameModeInventoriesInventory_pre implements GameModeInventoriesAPI {

    private final static String textBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private final static char[] alphabetBase64 = textBase64.toCharArray();
    private final static String regexBase64 = "[^" + textBase64 + "=]";
    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();
    GameModeInventoriesXPCalculator xpc;
    GameModeInventoriesArmour armour;

    @Override
    public void switchInventories(Player p, Inventory inventory, boolean savexp, boolean savearmour, GameMode newGM) {
        String name = p.getName();
        String currentGM = p.getGameMode().name();
        if (savexp) {
            xpc = new GameModeInventoriesXPCalculator(p);
        }
        if (savearmour) {
            armour = new GameModeInventoriesArmour();
        }
        String inv = GameModeInventoriesInventory_v1_4_6.toBase64(p.getInventory());
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            // get their current gamemode inventory from database
            String getQuery = "SELECT id FROM inventories WHERE player = '" + name + "' AND gamemode = '" + currentGM + "'";
            ResultSet rsInv = statement.executeQuery(getQuery);
            int id = 0;
            if (rsInv.next()) {
                // update it with their current inventory
                id = rsInv.getInt("id");
                String updateQuery = "UPDATE inventories SET inventory = '" + inv + "' WHERE id = " + id;
                statement.executeUpdate(updateQuery);
            } else {
                // they haven't got an inventory saved yet so make one with their current inventory
                String invQuery = "INSERT INTO inventories (player, gamemode, inventory) VALUES ('" + name + "','" + currentGM + "','" + inv + "')";
                statement.executeUpdate(invQuery);
                ResultSet idRS = statement.getGeneratedKeys();
                if (idRS.next()) {
                    id = idRS.getInt(1);
                }
            }
            rsInv.close();
            if (savexp) {
                // get players XP
                int a = xpc.getCurrentExp();
                String xpQuery = "UPDATE inventories SET xp = '" + a + "' WHERE id = " + id;
                statement.executeUpdate(xpQuery);
            }
            if (savearmour) {
                // get players XP
                Inventory armor = armour.getArmorInventory(p.getInventory());
                String arm = GameModeInventoriesInventory_v1_4_6.toBase64(armor);
                String armourQuery = "UPDATE inventories SET armour = '" + arm + "' WHERE id = " + id;
                statement.executeUpdate(armourQuery);
            }
            // check if they have an inventory for the new gamemode
            String getNewQuery = "SELECT inventory, xp, armour FROM inventories WHERE player = '" + name + "' AND gamemode = '" + newGM + "'";
            ResultSet rsNewInv = statement.executeQuery(getNewQuery);
            int amount;
            String savedarmour = "";
            if (rsNewInv.next()) {
                // set their inventory to the saved one
                String base64 = rsNewInv.getString("inventory");
                Inventory i = GameModeInventoriesInventory_v1_4_6.fromBase64(base64);
                p.getInventory().setContents(i.getContents());
                amount = rsNewInv.getInt("xp");
                if (savearmour) {
                    savedarmour = rsNewInv.getString("armour");
                    Inventory a = GameModeInventoriesInventory_v1_4_6.fromBase64(savedarmour);
                    armour.setArmour(p, a);
                }
            } else {
                // start with an empty inventory
                p.getInventory().clear();
                if (savearmour) {
                    p.getInventory().setBoots(null);
                    p.getInventory().setChestplate(null);
                    p.getInventory().setLeggings(null);
                    p.getInventory().setHelmet(null);
                }
                amount = 0;
            }
            rsNewInv.close();
            statement.close();
            if (savexp) {
                xpc.setExp(amount);
            }
        } catch (SQLException e) {
            System.err.println("Could not save inventory on gamemode change, " + e);
        }
    }

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
                inventory.setItem(i, new CraftItemStack(net.minecraft.server.ItemStack.a(inputObject)));
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