package me.eccentric_nz.plugins.gamemodeinventories;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface GameModeInventoriesInventory_api {

    public void switchInventories(Player p, Inventory inventory, boolean savexp, boolean savearmour, boolean saveender, boolean potions, GameMode newGM);

    public void saveOnDeath(Player p);

    public void restoreOnSpawn(Player p);

    public boolean isInstanceOf(Entity e);

    public boolean isInstanceOf(InventoryHolder h);
}
