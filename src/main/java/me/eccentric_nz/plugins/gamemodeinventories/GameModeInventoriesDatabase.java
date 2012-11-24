package me.eccentric_nz.plugins.gamemodeinventories;

import java.sql.Connection;
import java.sql.DriverManager;
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
            String queryInventories = "CREATE TABLE IF NOT EXISTS inventories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, player TEXT, gamemode TEXT, inventory TEXT)";
            statement.executeUpdate(queryInventories);
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
