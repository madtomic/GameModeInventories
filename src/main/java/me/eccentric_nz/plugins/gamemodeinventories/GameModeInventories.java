package me.eccentric_nz.plugins.gamemodeinventories;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GameModeInventories extends JavaPlugin implements Listener {

    protected static GameModeInventories plugin;
    PluginManager pm = Bukkit.getServer().getPluginManager();
    GameModeInventoriesListener GMListener = new GameModeInventoriesListener(this);
    GameModeInventoriesDeath DeathListener = new GameModeInventoriesDeath(this);
    private GameModeInventoriesCommands commando;
    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();

    @Override
    public void onEnable() {
        plugin = this;
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                System.err.println(GameModeInventoriesConstants.MY_PLUGIN_NAME + "Could not create directory!");
                System.err.println(GameModeInventoriesConstants.MY_PLUGIN_NAME + "Requires you to manually make the GameModeInventories/ directory!");
            }
            getDataFolder().setWritable(true);
            getDataFolder().setExecutable(true);
        }
        this.saveDefaultConfig();

        try {
            String path = getDataFolder() + File.separator + "GMI.db";
            service.setConnection(path);
            service.createTables();
        } catch (Exception e) {
            debug("Connection and Tables Error: " + e);
        }

        pm.registerEvents(GMListener, this);
        pm.registerEvents(DeathListener, this);
        commando = new GameModeInventoriesCommands(plugin);
        getCommand("gmi").setExecutor(commando);

        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        if (!getConfig().contains("xp")) {
            getConfig().set("xp", true);
            saveConfig();
        }
        if (!getConfig().contains("armor")) {
            getConfig().set("armor", true);
            saveConfig();
        }
    }

    @Override
    public void onDisable() {
        this.saveConfig();
        try {
            service.connection.close();
        } catch (Exception e) {
            System.err.println(GameModeInventoriesConstants.MY_PLUGIN_NAME + " Could not close database connection: " + e);
        }
    }

    public void debug(Object o) {
        if (getConfig().getBoolean("debug") == true) {
            System.out.println("[QuickDraw Debug] " + o);
        }
    }
}