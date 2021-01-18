package ru.geekbrains.server.dbservices;

import ru.geekbrains.server.NetworkServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {

    private static final String dbDriver = "com.mysql.cj.jdbc.Driver";
    private static final String dbUrl = "jdbc:mysql://localhost:3306/";
    private static final String dbUsername = "root";
    private static final String dbPassword = "gtr120519";
    private static final String dbName = "chat_users";

    private static final String timeZoneConfiguration = "?serverTimezone=Europe/Moscow&useSSL=false";

    public DBConnector() {
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка загрузки драйвера базы данных!", e);
        }
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(dbUrl + dbName + timeZoneConfiguration, dbUsername, dbPassword);
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка создания подключения к базе данных!", e);
        }
        return null;
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка закрытия соединения с базой данных", e);
        }
    }
}
