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

    private ClientController controller;
    private BufferedWriter fileWriter;
    private String historyFileName;
    private File historyFile;

    public ChatHistory(ClientController controller, String userID) {
        this.controller = controller;
        historyFileName = ORIGIN_NAME + userID + FILE_EXTENSION;
        this.historyFile = new File(historyFileName);
        startWriteChatHistory();
    }

    /**
     * Запускает выходной поток для записи в файл
     */
    public void startWriteChatHistory() {
        try {
            fileWriter = new BufferedWriter(new FileWriter(historyFile, true));
        } catch (IOException e) {
            System.err.println("Ошибка при создании потока записи в файл истории!");
            e.printStackTrace();
        }
    }

    /**
     * Записывает историю чата в файл
     *
     * @param message - сообщение для записи
     */
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

    /**
     * Считывает количество строк с конца файла, заданное в COUNT_STRINGS
     */
    public void readHistory() {
        try {
            // если файл пустой или отсутствует, выходим
            if (!historyFile.exists() || historyFile.length() == 0) {
                return;
            }
            // Получаем массив строк из файла истории, проверяем размерность
            List<String> stringsOfHistory = Files.readAllLines(Paths.get(historyFileName));
            int i = 0;
            if (stringsOfHistory.size() > COUNT_STRINGS) {
                i = stringsOfHistory.size() - COUNT_STRINGS;
            }
            // Выводим на печать строки из файла истории
            for (int j = i; j < stringsOfHistory.size(); j++) {
                controller.getClientChat().updateChatText(stringsOfHistory.get(j));
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения из файла истории!");
            e.printStackTrace();
        }
    }

    /**
     * Останавливает поток записи
     */
    public void stopWriteChatHistory() {
        try {
            if (fileWriter != null) fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
