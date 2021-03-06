package ru.geekbrains.client.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ChatHistory {

    private static final int COUNT_STRINGS = 100;
    private static final String ORIGIN_NAME = "history_";
    private static final String FILE_EXTENSION = ".txt";

    private final ClientController controller;
    private final String historyFileName;
    private final File historyFile;
    private BufferedWriter fileWriter;

    public ChatHistory(ClientController controller, String userID) {
        this.controller = controller;
        this.historyFileName = ORIGIN_NAME + userID + FILE_EXTENSION;
        this.historyFile = new File(historyFileName);
        startWriteChatHistory();
    }

    public void startWriteChatHistory() {
        try {
            fileWriter = new BufferedWriter(new FileWriter(historyFile, true));
        } catch (IOException e) {
            System.err.println("Ошибка при создании потока записи в файл истории!");
            e.printStackTrace();
        }
    }

    public void writeHistory(String message) {
        try {
            fileWriter.write(message);
            fileWriter.newLine();
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("Ошибка записи данных в файл истории!");
            e.printStackTrace();
        }
    }

    public void readHistory() {
        try {
            if (!historyFile.exists() || historyFile.length() == 0) {
                return;
            }
            final List<String> stringsOfHistory = Files.readAllLines(Paths.get(historyFileName));
            int i = 0;
            if (stringsOfHistory.size() > COUNT_STRINGS) {
                i = stringsOfHistory.size() - COUNT_STRINGS;
            }

            for (int j = i; j < stringsOfHistory.size(); j++) {
                controller.getClientChat().updateChatText(stringsOfHistory.get(j));
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения из файла истории!");
            e.printStackTrace();
        }
    }

    public void stopWriteChatHistory() {
        try {
            if (fileWriter != null) fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
