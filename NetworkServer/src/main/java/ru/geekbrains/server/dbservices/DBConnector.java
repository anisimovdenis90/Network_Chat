package ru.geekbrains.server.dbservices;

import org.apache.logging.log4j.Logger;
import ru.geekbrains.server.NetworkServer;
import java.sql.*;

public class DBConnector {

    private final String dbDriver = "com.mysql.cj.jdbc.Driver";
    private final String dbUrl = "jdbc:mysql://localhost:3306/";
    private final String dbUsername = "root";
    private final String dbPassword = "gtr120519";
    private final String dbName = "chat_users";

    private final String timeZoneConfiguration = "?serverTimezone=Europe/Moscow&useSSL=false";

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
