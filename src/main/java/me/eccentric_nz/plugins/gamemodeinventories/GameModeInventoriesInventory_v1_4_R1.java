/*
 * Kristian S. Stangeland aadnk
 * Norway
 * kristian@comphenix.net
 * thtp://www.comphenix.net/
 */
package me.eccentric_nz.plugins.gamemodeinventories;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.minecraft.server.v1_4_R1.NBTBase;
import net.minecraft.server.v1_4_R1.NBTTagCompound;
import net.minecraft.server.v1_4_R1.NBTTagList;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_4_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_4_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public class GameModeInventoriesInventory_v1_4_R1 implements GameModeInventoriesInventory_api {

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
            if (saveender) {
                // get players enderchest
                Inventory ec = p.getEnderChest();
                if (ec != null) {
                    String ender = toBase64(ec);
                    String enderQuery = "UPDATE inventories SET enderchest = '" + ender + "' WHERE id = " + id;
                    statement.executeUpdate(enderQuery);
                }
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
                if (saveender) {
                    savedender = rsNewInv.getString("enderchest");
                    if (savedender.equals("[Null]") || savedender.equals("") || savedender.isEmpty()) {
                        // empty inventory
                        savedender = "CQAACgAAABsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
                    }
                    Inventory a = fromBase64(savedender);
                    Inventory echest = p.getEnderChest();
                    echest.setContents(a.getContents());
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
                if (saveender) {
                    Inventory echest = p.getEnderChest();
                    echest.clear();
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
                CraftItemStack.asNMSCopy(craft).save(outputObject);
            }
            itemList.add(outputObject);
        }

        // Now save the list
        NBTBase.a(itemList, dataOutput);

        // Serialize that array
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    public static Inventory fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
        Inventory inventory = new CraftInventoryCustom(null, itemList.size());

        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);

            if (!inputObject.isEmpty()) {
                inventory.setItem(i, CraftItemStack.asCraftMirror(
                        net.minecraft.server.v1_4_R1.ItemStack.createStack(inputObject)));
            }
        }

        // Serialize that array
        return inventory;
    }

    private static CraftItemStack getCraftVersion(ItemStack stack) {
        if (stack instanceof CraftItemStack) {
            return (CraftItemStack) stack;
        } else if (stack != null) {
            return CraftItemStack.asCraftCopy(stack);
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

    public boolean isInstanceOf(Entity e) {
        if (e instanceof PoweredMinecart || e instanceof StorageMinecart || e instanceof ItemFrame) {
            return true;
        }
        return false;
    }

    public boolean isInstanceOf(InventoryHolder h) {
        return false;
    }
}
