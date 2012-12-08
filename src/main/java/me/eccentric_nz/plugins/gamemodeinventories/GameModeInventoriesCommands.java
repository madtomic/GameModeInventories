package me.eccentric_nz.plugins.gamemodeinventories;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GameModeInventoriesCommands implements CommandExecutor {

    private GameModeInventories plugin;

    public GameModeInventoriesCommands(GameModeInventories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("gmi")) {
            if (args.length == 0) {
                sender.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + GameModeInventoriesConstants.HELP.split("\n"));
                return true;
            }
            if (sender.hasPermission("gamemodeinventories.admin")) {
                if (args.length == 1 && args[0].equalsIgnoreCase("save_on_death")) {
                    boolean bool = !plugin.getConfig().getBoolean("save_on_death");
                    plugin.getConfig().set("save_on_death", bool);
                    sender.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "save_on_death was set to: " + bool);
                    plugin.saveConfig();
                    return true;
                }
                if (args.length == 1 && args[0].equalsIgnoreCase("xp")) {
                    boolean bool = !plugin.getConfig().getBoolean("xp");
                    plugin.getConfig().set("xp", bool);
                    sender.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "xp was set to: " + bool);
                    plugin.saveConfig();
                    return true;
                }
                if (args.length == 1 && args[0].equalsIgnoreCase("armor")) {
                    boolean bool = !plugin.getConfig().getBoolean("armor");
                    plugin.getConfig().set("armor", bool);
                        sender.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "armor was set to: " + bool);
                    plugin.saveConfig();
                    return true;
                }
                if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
                    boolean bool = !plugin.getConfig().getBoolean("debug");
                    plugin.getConfig().set("debug", bool);
                    sender.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "Debugging was set to: " + bool);
                    plugin.saveConfig();
                    return true;
                }
            } else {
                sender.sendMessage(GameModeInventoriesConstants.MY_PLUGIN_NAME + "You do not have permission to run that command!");
                return true;
            }
        }
        return false;
    }
}
