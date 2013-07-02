package me.eccentric_nz.plugins.gamemodeinventories;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GameModeInventories extends JavaPlugin implements Listener {

    public GameModeInventoriesInventory_api inventoryHandler;
    protected static GameModeInventories plugin;
    PluginManager pm = Bukkit.getServer().getPluginManager();
    GameModeInventoriesListener GMListener = new GameModeInventoriesListener(this);
    GameModeInventoriesDeath DeathListener = new GameModeInventoriesDeath(this);
    private GameModeInventoriesCommands commando;
    GameModeInventoriesDatabase service = GameModeInventoriesDatabase.getInstance();
    boolean found = false;

    @Override
    public void onEnable() {
        if (loadClasses()) {
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
            GameModeInventoriesConfig tc = new GameModeInventoriesConfig(this);
            tc.checkConfig();

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
        }
    }

    @Override
    public void onDisable() {
        this.saveConfig();
        try {
            service.connection.close();
        } catch (Exception e) {
            if (found) {
                System.err.println("[GameModeInventories] Could not close database connection: " + e);
            }
        }
    }

    public void debug(Object o) {
        if (getConfig().getBoolean("debug") == true) {
            System.out.println("[GameModeInventories Debug] " + o);
        }
    }

    private boolean loadClasses() {
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
            if (GameModeInventoriesInventory_api.class.isAssignableFrom(clazz)) { // Make sure it actually implements IOP
                this.inventoryHandler = (GameModeInventoriesInventory_api) clazz.getConstructor().newInstance(); // Set our handler
            }
            found = true;
            System.out.println("[GameModeInventories] Loading support for CB " + version);
        } catch (final Exception e) {
            this.getLogger().severe("[GameModeInventories] Could not find support for this CraftBukkit version.");
            this.getLogger().info("[GameModeInventories] Check for updates at http://dev.bukkit.org/server-mods/gamemodeinventories/");
            this.setEnabled(false);
            found = false;
        }
        return found;
    }
}
