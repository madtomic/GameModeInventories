package me.eccentric_nz.plugins.gamemodeinventories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GameModeInventoriesDatabase {

    private static GameModeInventoriesDatabase instance = new GameModeInventoriesDatabase();
    public Connection connection = null;
    public Statement statement;
    private GameModeInventories plugin;

    public static synchronized GameModeInventoriesDatabase getInstance() {
        return instance;
    }

    public void setConnection(String path) throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    public Connection getConnection() {
        return connection;
    }

    public void createTables() {
        try {
            statement = connection.createStatement();
            String queryInventories = "CREATE TABLE IF NOT EXISTS inventories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, player TEXT, gamemode TEXT, inventory TEXT, xp REAL, armour TEXT)";
            statement.executeUpdate(queryInventories);
            // update inventories if there is no xp column
            String queryXP = "SELECT sql FROM sqlite_master WHERE tbl_name = 'inventories' AND sql LIKE '%xp REAL%'";
            ResultSet rsXP = statement.executeQuery(queryXP);
            if (!rsXP.next()) {
                String queryAlter = "ALTER TABLE inventories ADD xp REAL";
                statement.executeUpdate(queryAlter);
                System.out.println(GameModeInventoriesConstants.MY_PLUGIN_NAME + "Adding xp to database!");
            }
            // update inventories if there is no armour column
            String queryArmour = "SELECT sql FROM sqlite_master WHERE tbl_name = 'inventories' AND sql LIKE '%armour TEXT%'";
            ResultSet rsArmour = statement.executeQuery(queryArmour);
            if (!rsArmour.next()) {
                String queryAlter2 = "ALTER TABLE inventories ADD armour TEXT";
                statement.executeUpdate(queryAlter2);
                System.out.println(GameModeInventoriesConstants.MY_PLUGIN_NAME + "Adding armour to database!");
            }
            statement.close();
        } catch (SQLException e) {
            plugin.debug("Create table error: " + e);
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Clone is not allowed.");
    }
}
