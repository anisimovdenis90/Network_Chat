package ru.geekbrains.server.dbservices.auth;

import ru.geekbrains.server.NetworkServer;
import ru.geekbrains.server.dbservices.DBConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService implements Authenticator {

    private DBConnector dbConnector;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    public AuthService(ru.geekbrains.server.dbservices.DBConnector DBConnector) {
        this.dbConnector = DBConnector;
        NetworkServer.getInfoLogger().info("Сервис авторизации успешно запущен");
    }

    @Override
    public String[] getUserNickAndIDByLoginAndPassword(String login, String password) {
        final String[] userNickAndID = new String[2];
        connection = dbConnector.getConnection();
        try {
            preparedStatement = connection.prepareStatement(
                    "SELECT id, nickname FROM users WHERE login = ? AND password = ?"
            );
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            resultSet = preparedStatement.executeQuery();
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

    public int signUpUser(String login, String password, String nickname) {
        connection = dbConnector.getConnection();
        try {
            preparedStatement = connection.prepareStatement(
                    "INSERT INTO users (nickname, login, password) VALUES (?, ?, ?)"
            );
            preparedStatement.setString(1, nickname);
            preparedStatement.setString(2, login);
            preparedStatement.setString(3, password);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка регистрации пользователя!", e);
        } finally {
            close();
        }
        return 0;
    }

    @Override
    public int changeNickname(String oldNickname, String newNickname) {
        connection = dbConnector.getConnection();
        try {
            preparedStatement = connection.prepareStatement(
                    "UPDATE users SET nickname = ? WHERE nickname = ?"
            );
            preparedStatement.setString(1, newNickname);
            preparedStatement.setString(2, oldNickname);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка изменения данных в базе", e);
        } finally {
            close();
        }
        return 0;
    }

    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().fatal("Ошибка завершения работы с базой данных", e);
        } finally {
            dbConnector.closeConnection(connection);
        }
    }
}
