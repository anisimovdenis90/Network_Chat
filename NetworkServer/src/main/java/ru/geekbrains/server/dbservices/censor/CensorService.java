package ru.geekbrains.server.dbservices.censor;

import ru.geekbrains.server.NetworkServer;
import ru.geekbrains.server.dbservices.DBConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CensorService implements Censor {

    private final DBConnector dbConnector;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    private static final String BAD_WORD_REPLACER = "<censured>";
    private static final String DB_TABLE_NAME = "cens_words";
    private static final String DB_COLUMN_NAME = "bad_word";

    private static final String SQL = String.format("SELECT * FROM %s WHERE %s = ?", DB_TABLE_NAME, DB_COLUMN_NAME);

    public CensorService(ru.geekbrains.server.dbservices.DBConnector DBConnector) {
        this.dbConnector = DBConnector;
        NetworkServer.getInfoLogger().info("Сервис цензуры запущен");
    }

    @Override
    public String messageCensor(String message) {
        connection = dbConnector.getConnection();
        final String[] wordsToCheck = message.toLowerCase().split("\\s+");
        for (String word : wordsToCheck) {
            if ((word.length() > 2)) {
                if (findWordInDB(word.replace("ё", "е"))) {
                    NetworkServer.getInfoLogger().info("Сервис цензуры сработал!");
                    message = message.replace(word, word.length() > 4 ?
                            String.format("%s%s%s", word.substring(0, 2), BAD_WORD_REPLACER, word.substring(word.length() - 2)) :
                            String.format("%c%s%c", word.charAt(0), BAD_WORD_REPLACER, word.charAt(word.length() - 1)));
                }
            }
        }
        closeAll();
        return message;
    }

    private boolean findWordInDB(String wordsToCheck) {
        try {
            preparedStatement = connection.prepareStatement(SQL);
            preparedStatement.setString(1, wordsToCheck);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            NetworkServer.getFatalLogger().error("Ошибка получения данных сервером цензуры", e);
        } finally {
            close();
        }
        return false;
    }

    @Override
    public boolean isCensured(String message) {
        return message.equals(messageCensor(message));
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
            NetworkServer.getFatalLogger().error("Ошибка завершения работы с базой данных", e);
        }
    }

    public void closeAll() {
        try {
            close();
        } finally {
            dbConnector.closeConnection(connection);
        }
    }
}