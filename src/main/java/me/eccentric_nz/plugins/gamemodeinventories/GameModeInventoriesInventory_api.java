package me.eccentric_nz.plugins.gamemodeinventories;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface GameModeInventoriesInventory_api {

    public void switchInventories(Player p, Inventory inventory, boolean savexp, boolean savearmour, GameMode newGM);
}