package me.eccentric_nz.plugins.gamemodeinventories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.inventory.Inventory;

public class GameModeInventoriesListener implements Listener {

    private GameModeInventories plugin;
    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();
    GameModeInventoriesXPCalculator xpc;

    public GameModeInventoriesListener(GameModeInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player p = event.getPlayer();
        String name = p.getName();
        String currentGM = p.getGameMode().name();
        GameMode newGM = event.getNewGameMode();
        if (p.hasPermission("gamemodeinventories.use")) {
            boolean savexp = plugin.getConfig().getBoolean("xp");
            if (savexp) {
                xpc = new GameModeInventoriesXPCalculator(p);
            }
            String inv = GameModeInventoriesInventory.toBase64(p.getInventory());
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
                // check if they have an inventory for the new gamemode
                String getNewQuery = "SELECT inventory, xp FROM inventories WHERE player = '" + name + "' AND gamemode = '" + newGM + "'";
                ResultSet rsNewInv = statement.executeQuery(getNewQuery);
                int amount;
                if (rsNewInv.next()) {
                    // set their inventory to the saved one
                    String base64 = rsNewInv.getString("inventory");
                    Inventory i = GameModeInventoriesInventory.fromBase64(base64);
                    p.getInventory().setContents(i.getContents());
                    amount = rsNewInv.getInt("xp");
                } else {
                    // start with an empty inventory
                    p.getInventory().clear();
                    amount = 0;
                }
                rsNewInv.close();
                statement.close();
                if (savexp) {
                    xpc.setExp(amount);
                }
            } catch (SQLException e) {
                plugin.debug("Could not save inventory on gamemode change, " + e);
            }
        }
    }
}
