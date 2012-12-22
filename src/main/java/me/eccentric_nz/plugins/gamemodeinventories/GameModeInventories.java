package me.eccentric_nz.plugins.gamemodeinventories;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GameModeInventories extends JavaPlugin implements Listener {

    public GameModeInventoriesAPI inventoryHandler;
    protected static GameModeInventories plugin;
    PluginManager pm = Bukkit.getServer().getPluginManager();
    GameModeInventoriesListener GMListener = new GameModeInventoriesListener(this);
    GameModeInventoriesDeath DeathListener = new GameModeInventoriesDeath(this);
    private GameModeInventoriesCommands commando;
    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();

    @Override
    public void onEnable() {
        String packageName = this.getServer().getClass().getPackage().getName();
        // Get full package string of CraftServer.
        // org.bukkit.craftbukkit.versionstring (or for pre-refactor, just org.bukkit.craftbukkit
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        // Get the last element of the package
        if (version.equals("craftbukkit")) { // If the last element of the package was "craftbukkit" we are now pre-refactor
            version = "pre";
        }
        try {
            final Class<?> clazz = Class.forName("me.eccentric_nz.plugins.gamemodeinventories.GameModeInventoriesInventory_" + version);
            // Check if we have a NMSHandler class at that location.
            if (GameModeInventoriesAPI.class.isAssignableFrom(clazz)) { // Make sure it actually implements IOP
                this.inventoryHandler = (GameModeInventoriesAPI) clazz.getConstructor().newInstance(); // Set our handler
            }
        } catch (final Exception e) {
            this.getLogger().severe("Could not find support for this CraftBukkit version.");
            this.getLogger().info("Check for updates at http://dev.bukkit.org/server-mods/gamemodeinventories/");
            this.setEnabled(false);
            return;
        }
        this.getLogger().log(Level.INFO, "Loading support for CB {0}", version);
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
            System.out.println("[GameModeInventories Debug] " + o);
        }
    }
}