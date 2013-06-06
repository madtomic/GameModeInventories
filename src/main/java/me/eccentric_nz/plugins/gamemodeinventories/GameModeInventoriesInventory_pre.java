package me.eccentric_nz.plugins.gamemodeinventories;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public class GameModeInventoriesInventory_pre implements GameModeInventoriesInventory_api {

    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();
    GameModeInventoriesXPCalculator xpc;

    @Override
    public void switchInventories(Player p, Inventory inventory, boolean savexp, boolean savearmour, boolean saveender, boolean potions, GameMode newGM) {
        String name = p.getName();
        String currentGM = p.getGameMode().name();
        if (savexp) {
            xpc = new GameModeInventoriesXPCalculator(p);
        }
        String inv = toBase64(p.getInventory());
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
                // get players armour
                Inventory armor = getArmorInventory(p.getInventory());
                String arm = toBase64(armor);
                String armourQuery = "UPDATE inventories SET armour = '" + arm + "' WHERE id = " + id;
                statement.executeUpdate(armourQuery);
            }
            if (potions && currentGM.equals("CREATIVE") && newGM.equals(GameMode.SURVIVAL)) {
                // remove all potion effects
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
            }
            // check if they have an inventory for the new gamemode
            String getNewQuery = "SELECT inventory, xp, armour, enderchest FROM inventories WHERE player = '" + name + "' AND gamemode = '" + newGM + "'";
            ResultSet rsNewInv = statement.executeQuery(getNewQuery);
            int amount;
            String savedarmour;
            String savedender;
            if (rsNewInv.next()) {
                // set their inventory to the saved one
                String base64 = rsNewInv.getString("inventory");
                Inventory i = fromBase64(base64);
                p.getInventory().setContents(i.getContents());
                amount = rsNewInv.getInt("xp");
                if (savearmour) {
                    savedarmour = rsNewInv.getString("armour");
                    Inventory a = fromBase64(savedarmour);
                    setArmour(p, a);
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

    @Override
    public void saveOnDeath(Player p) {
        String name = p.getName();
        String gm = p.getGameMode().name();
        String inv = toBase64(p.getInventory());
        Inventory armor = getArmorInventory(p.getInventory());
        String arm = toBase64(armor);
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            // get their current gamemode inventory from database
            String getQuery = "SELECT id FROM inventories WHERE player = '" + name + "' AND gamemode = '" + gm + "'";
            ResultSet rsInv = statement.executeQuery(getQuery);
            if (rsInv.next()) {
                // update it with their current inventory
                int id = rsInv.getInt("id");
                String updateQuery = "UPDATE inventories SET inventory = '" + inv + "', armour = '" + arm + "' WHERE id = " + id;
                statement.executeUpdate(updateQuery);
                rsInv.close();
            } else {
                // they haven't got an inventory saved yet so make one with their current inventory
                String invQuery = "INSERT INTO inventories (player, gamemode, inventory, armour) VALUES ('" + name + "','" + gm + "','" + inv + "','" + arm + "')";
                statement.executeUpdate(invQuery);
            }
            statement.close();
        } catch (SQLException e) {
            System.err.println("Could not save inventories on player death, " + e);
        }
    }

    @Override
    public void restoreOnSpawn(Player p) {
        String name = p.getName();
        String gm = p.getGameMode().name();
        // restore their inventory
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            // get their current gamemode inventory from database
            String getQuery = "SELECT inventory, armour FROM inventories WHERE player = '" + name + "' AND gamemode = '" + gm + "'";
            ResultSet rsInv = statement.executeQuery(getQuery);
            if (rsInv.next()) {
                // set their inventory to the saved one
                String base64 = rsInv.getString("inventory");
                Inventory i = fromBase64(base64);
                p.getInventory().setContents(i.getContents());
                String savedarmour = rsInv.getString("armour");
                Inventory a = fromBase64(savedarmour);
                setArmour(p, a);
            }
            rsInv.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Could not restore inventories on respawn, " + e);
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
    }

    public static Inventory fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(data, 32).toByteArray());
        //ByteArrayInputStream inputStream = new ByteArrayInputStream(decodeBase64(data));
        NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
        Inventory inventory = new CraftInventoryCustom(null, itemList.size());
        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
            // IsEmpty
            if (Bukkit.getServer().getBukkitVersion().contains("1.2.5-R")) {
                if (!itemList.get(i).equals("")) {
                    inventory.setItem(i, new CraftItemStack(net.minecraft.server.ItemStack.a(inputObject)));
                }
            } else {
                if (!inputObject.d()) {
                    inventory.setItem(i, new CraftItemStack(net.minecraft.server.ItemStack.a(inputObject)));
                }
            }
        }
        // Serialize that array
        return inventory;
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

    public Inventory getArmorInventory(PlayerInventory inventory) {
        ItemStack[] armor = inventory.getArmorContents();
        CraftInventoryCustom storage = new CraftInventoryCustom(null, armor.length);
        for (int i = 0; i < armor.length; i++) {
            storage.setItem(i, armor[i]);
        }
        return storage;
    }

    public void setArmour(Player p, Inventory i) {
        ItemStack[] is = i.getContents();
        p.getInventory().setArmorContents(is);
    }
}
