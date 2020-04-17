package ru.geekbrains.server.dbservices.auth;

import ru.geekbrains.server.NetworkServer;
import ru.geekbrains.server.dbservices.DBConnector;

import java.sql.*;

public class AuthService implements Authenticator {

    private final DBConnector dbConnector;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    public AuthService(ru.geekbrains.server.dbservices.DBConnector DBConnector) {
        this.dbConnector = DBConnector;
        NetworkServer.getInfoLogger().info("Сервис авторизации успешно запущен");
    }

    /**
     * Возвращает никнейм и ID пользователя при авторизации
     *
     * @param login    - логин, введенный пользователем
     * @param password - пароль, введенный пользователем
     * @return String  - никнейм пользователя
     */
    @Override
    public synchronized String[] getUserNickAndIDByLoginAndPassword(String login, String password) {
        final String[] userNickAndID = new String[2];
        connection = dbConnector.getConnection();
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(String.format("SELECT id, nickname FROM users WHERE login = '%s' AND password = '%s'", login, password));
            while (resultSet.next()) {
                userNickAndID[0] = "id" + resultSet.getString("id");
                userNickAndID[1] = resultSet.getString("nickname");
            }
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка получения данных из базы!", e);
        } finally {
            close();
        }
        return userNickAndID;
    }

    /**
     * Изменяет никнейм пользователя в базе данных
     *
     * @param oldNickname - старый никнейм пользователя
     * @param newNickname - новый никнейм
     * @return int - кол-во измененных строк
     */
    @Override
    public synchronized int changeNickname(String oldNickname, String newNickname) {
        int countChanges = 0;
        connection = dbConnector.getConnection();
        try {
            statement = connection.createStatement();
            countChanges = statement.executeUpdate(String.format("UPDATE users SET nickname = '%s' WHERE nickname = '%s'", newNickname, oldNickname));
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка изменения данных в базе", e);
        }
        return countChanges;
    }

    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка завершения работы с базой данных", e);
        } finally {
            dbConnector.closeConnection(connection);
        }
    }
}
