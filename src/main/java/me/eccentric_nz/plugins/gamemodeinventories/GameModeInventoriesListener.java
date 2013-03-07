package me.eccentric_nz.plugins.gamemodeinventories;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeInventoriesListener implements Listener {

    private GameModeInventories plugin;

    public GameModeInventoriesListener(GameModeInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player p = event.getPlayer();
        GameMode newGM = event.getNewGameMode();
        if (p.hasPermission("gamemodeinventories.use")) {
            boolean savexp = plugin.getConfig().getBoolean("xp");
            boolean savearmour = plugin.getConfig().getBoolean("armor");
            boolean saveenderchest = plugin.getConfig().getBoolean("enderchest");
            plugin.inventoryHandler.switchInventories(p, p.getInventory(), savexp, savearmour, saveenderchest, newGM);
        }
    }
}
