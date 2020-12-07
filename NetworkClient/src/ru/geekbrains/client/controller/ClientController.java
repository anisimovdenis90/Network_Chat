package ru.geekbrains.client.controller;

import ru.geekbrains.client.Command;
import ru.geekbrains.client.model.NetworkService;
import ru.geekbrains.client.view.AuthDialog;
import ru.geekbrains.client.view.ClientChat;

import java.io.IOException;
import java.util.List;

public class ClientController {

    private final NetworkService networkService;
    private final AuthDialog authDialog;
    private final ClientChat clientChat;
    private ChatHistory chatHistory;
    private String nickname;

    public ClientController(String serverHost, int serverPort) {
        this.networkService = new NetworkService(serverHost, serverPort);
        this.authDialog = new AuthDialog(this);
        this.clientChat = new ClientChat(this);
    }

    public void setUserName(String nickname) {
        this.nickname = nickname;
    }

    public String getUsername() {
        return nickname;
    }

    public ClientChat getClientChat() {
        return clientChat;
    }

    public ChatHistory getChatHistory() {
        return chatHistory;
    }

    public void runApplication() throws IOException {
        connectToServer();
        runAuthProcess();
    }

    private void connectToServer() throws IOException {
        try {
            networkService.connect(this);
        } catch (IOException e) {
            System.err.println("Невозможно подключиться к серверу");
            throw e;
        }
    }

    private void runAuthProcess() {
        // Задаем никнейм и открываем окно чата при успешной авторизации
        networkService.setSuccessfulAuthEvent(new AuthEvent() {
            @Override
            public void authIsSuccessful(String nickname, String userID) {
                ClientController.this.setUserName(nickname);
                // Задаем заголовок окна чата
                chatHistory = new ChatHistory(ClientController.this, userID);
                chatHistory.readHistory();
                clientChat.setTitle(nickname);
                ClientController.this.openChat();
            }
        });
        // Отображаем окно ввода данных для авторизации
        authDialog.setVisible(true);
    }

    private void openChat() {
        // Убираем окно авторизации
        authDialog.dispose();
        // Задаем обработчик полученных сообщений от сервера
        networkService.setMessageHandler(new MessageHandler() {
            @Override
            public void handle(String message) {
                clientChat.appendMessage(message);
            }
        });
        // Запускаем окно чата
        clientChat.setVisible(true);
    }

    public void sendAuthMessage(String login, String password) throws IOException {
        networkService.sendCommand(Command.authCommand(login, password));
    }

    public void sendEndMessage() {
        try {
            networkService.sendCommand(Command.endCommand());
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessageToAllUsers(String message) {
        try {
            networkService.sendCommand(Command.broadcastMessageCommand(message));
        } catch (IOException e) {
            showErrorMessage("Ошибка отправки сообщения!");
            e.printStackTrace();
        }
    }

    public void sendPrivateMessage(String username, String message, String sender) {
        try {
            networkService.sendCommand(Command.privateMessageCommand(username, message, sender));
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    public void sendChangeNickNameMessage(String newNickname) {
        try {
            networkService.sendCommand(Command.changeNicknameMessageCommand(this.getUsername(), newNickname));
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    public void shutdown() {
        // Останавливаем поток чтения из файла истории
        chatHistory.stopWriteChatHistory();
        // Закрывается соединение
        networkService.close();
    }

    public void showErrorMessage(String errorMessage) {
        if ((clientChat.isActive())) {
            clientChat.showError(errorMessage);
        } else if (authDialog.isActive()) {
            authDialog.showError(errorMessage);
        } else {
            System.err.println(errorMessage);
        }
    }

    public void showErrorAndClose(String errorMessage) {
        authDialog.showErrorAndClose(errorMessage);
    }

    public void showMessage(String message) {
        clientChat.showMessage(message);
    }

    public void updateNickname(String nickname) {
        clientChat.updateTitle(nickname);
    }

    public void updateUsersList(List<String> users) {
        // Удаляем из списка текущего пользователя
        users.remove(nickname);
        // Добавляем строку 'отправить всем'
        users.add(0, "All");
        clientChat.updateUsers(users);
    }

    public void updateTimeoutLabel(String message) {
        authDialog.updateTimeOutAuthLabel(message);
    }
}
