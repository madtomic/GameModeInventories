package me.eccentric_nz.plugins.gamemodeinventories;

import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class GameModeInventoriesArmour {

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