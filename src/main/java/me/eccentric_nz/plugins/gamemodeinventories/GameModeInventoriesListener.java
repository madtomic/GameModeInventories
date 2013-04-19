package me.eccentric_nz.plugins.gamemodeinventories;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class GameModeInventoriesListener implements Listener {

    private GameModeInventories plugin;
    List<Material> containers = new ArrayList<Material>();

    public GameModeInventoriesListener(GameModeInventories plugin) {
        this.plugin = plugin;
        containers.add(Material.CHEST);
        containers.add(Material.DISPENSER);
        containers.add(Material.FURNACE);
        containers.add(Material.ENDER_CHEST);
        containers.add(Material.TRAPPED_CHEST);
        containers.add(Material.ANVIL);
        containers.add(Material.ENCHANTMENT_TABLE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player p = event.getPlayer();
        GameMode newGM = event.getNewGameMode();
        if (p.hasPermission("gamemodeinventories.use")) {
            boolean savexp = plugin.getConfig().getBoolean("xp");
            boolean savearmour = plugin.getConfig().getBoolean("armor");
            boolean saveenderchest = plugin.getConfig().getBoolean("enderchest");
            boolean potions = plugin.getConfig().getBoolean("remove_potions");
            plugin.inventoryHandler.switchInventories(p, p.getInventory(), savexp, savearmour, saveenderchest, potions, newGM);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(PlayerInteractEvent event) {
        if (plugin.getConfig().getBoolean("restrict_creative")) {
            Block b = event.getClickedBlock();
            if (b != null) {
                Material m = b.getType();
                Player p = event.getPlayer();
                GameMode gm = p.getGameMode();
                if (gm.equals(GameMode.CREATIVE) && containers.contains(m) && !p.hasPermission("gamemodeinventories.bypass") && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    event.setCancelled(true);
                    p.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "You are not allowed to access inventories in CREATIVE!");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (plugin.getConfig().getBoolean("no_drops")) {
            Player p = event.getPlayer();
            GameMode gm = p.getGameMode();
            if (gm.equals(GameMode.CREATIVE) && !p.hasPermission("gamemodeinventories.bypass")) {
                event.setCancelled(true);
                p.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "You are not allowed to drop items in CREATIVE!");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void noPickup(PlayerPickupItemEvent event) {
        if (plugin.getConfig().getBoolean("no_pickups")) {
            Player p = event.getPlayer();
            GameMode gm = p.getGameMode();
            if (gm.equals(GameMode.CREATIVE) && !p.hasPermission("gamemodeinventories.bypass")) {
                event.setCancelled(true);
                p.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "You are not allowed to pick up items in CREATIVE!");
            }
        }
    }
}
