package me.eccentric_nz.plugins.gamemodeinventories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;

public class GameModeInventoriesDeath implements Listener {

    private GameModeInventories plugin;
    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();

    public GameModeInventoriesDeath(GameModeInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        assert (event.getEntity() instanceof Player);
        Player p = (Player) event.getEntity();
        String name = p.getName();
        String gm = p.getGameMode().name();
        if (p.hasPermission("gamemodeinventories.death") && plugin.getConfig().getBoolean("save_on_death")) {
            // save their inventory
            String inv = GameModeInventoriesInventory.toBase64(p.getInventory());
            try {
                Connection connection = service.getConnection();
                Statement statement = connection.createStatement();
                // get their current gamemode inventory from database
                String getQuery = "SELECT id FROM inventories WHERE player = '" + name + "' AND gamemode = '" + gm + "'";
                ResultSet rsInv = statement.executeQuery(getQuery);
                if (rsInv.next()) {
                    // update it with their current inventory
                    int id = rsInv.getInt("id");
                    String updateQuery = "UPDATE inventories SET inventory = '" + inv + "' WHERE id = " + id;
                    statement.executeUpdate(updateQuery);
                    rsInv.close();
                } else {
                    // they haven't got an inventory saved yet so make one with their current inventory
                    String invQuery = "INSERT INTO inventories (player, gamemode, inventory) VALUES ('" + name + "','" + gm + "','" + inv + "')";
                    statement.executeUpdate(invQuery);
                }
                statement.close();
                event.getDrops().clear();
            } catch (SQLException e) {
                plugin.debug("Could not save inventories on player death, " + e);
            }
        }
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event) {
        final Player p = event.getPlayer();
        if (p.hasPermission("gamemodeinventories.death") && plugin.getConfig().getBoolean("save_on_death")) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    String name = p.getName();
                    String gm = p.getGameMode().name();
                    // restore their inventory
                    try {
                        Connection connection = service.getConnection();
                        Statement statement = connection.createStatement();
                        // get their current gamemode inventory from database
                        String getQuery = "SELECT inventory FROM inventories WHERE player = '" + name + "' AND gamemode = '" + gm + "'";
                        ResultSet rsInv = statement.executeQuery(getQuery);
                        if (rsInv.next()) {
                            // set their inventory to the saved one
                            String base64 = rsInv.getString("inventory");
                            Inventory i = GameModeInventoriesInventory.fromBase64(base64);
                            p.getInventory().setContents(i.getContents());
                        }
                        rsInv.close();
                        statement.close();
                    } catch (SQLException e) {
                        plugin.debug("Could not restore inventories on respawn, " + e);
                    }
                }
            });
        }
    }
}
