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

    /**
     * Запуск подключения клиенту к серверу
     *
     * @throws IOException - ошибка подключения
     */
    private void connectToServer() throws IOException {
        try {
            networkService.connect(this);
        } catch (IOException e) {
            System.err.println("Невозможно подключиться к серверу");
            throw e;
        }
    }

    /**
     * Запуск процесса авторизации для клиента
     */
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

    /**
     * Метод, отображающий окно чата
     */
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

    /**
     * Отправка сообщения авторизации, вызывается из окна авторизации (AuthDialog)
     *
     * @param login    - String логин
     * @param password - String пароль
     * @throws IOException - Exception при отправке сообщения
     */
    public void sendAuthMessage(String login, String password) throws IOException {
        networkService.sendCommand(Command.authCommand(login, password));
    }

    /**
     * Отправляет команду 'END' на сервер при закрытии чата
     */
    public void sendEndMessage() {
        try {
            networkService.sendCommand(Command.endCommand());
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Метод отправки сообщения всем пользователям, вызывается из окна чата
     *
     * @param message - текст сообщения
     */
    public void sendMessageToAllUsers(String message) {
        try {
            networkService.sendCommand(Command.broadcastMessageCommand(message));
        } catch (IOException e) {
            showErrorMessage("Ошибка отправки сообщения!");
            e.printStackTrace();
        }
    }

    /**
     * Метод отправления сообщения указанному контакту
     *
     * @param username - имя адресата
     * @param message  - текст сообщения
     */
    public void sendPrivateMessage(String username, String message, String sender) {
        try {
            networkService.sendCommand(Command.privateMessageCommand(username, message, sender));
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    /**
     * Отправка сообщения о смене никнейма
     *
     * @param newNickname - новый никнейм
     */
    public void sendChangeNickNameMessage(String newNickname) {
        try {
            networkService.sendCommand(Command.changeNicknameMessageCommand(this.getUsername(), newNickname));
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    /**
     * Закрывает соединение при отключении клиента
     */
    public void shutdown() {
        // Останавливаем поток чтения из файла истории
        chatHistory.stopWriteChatHistory();
        // Закрывается соединение
        networkService.close();
    }

    /**
     * Вызывает отображение сообщения об ошибке в соответствующем окне
     *
     * @param errorMessage - текст сообщения
     */
    public void showErrorMessage(String errorMessage) {
        if ((clientChat.isActive())) {
            clientChat.showError(errorMessage);
        } else if (authDialog.isActive()) {
            authDialog.showError(errorMessage);
        } else {
            System.err.println(errorMessage);
        }
    }

    /**
     * Отображает ошибку и закрывает клиент
     *
     * @param errorMessage - текст ошибки
     */
    public void showErrorAndClose(String errorMessage) {
        authDialog.showErrorAndClose(errorMessage);
    }

    public void showMessage(String message) {
        clientChat.showMessage(message);
    }

    public void updateNickname(String nickname) {
        clientChat.updateTitle(nickname);
    }

    /**
     * метод обновления списка пользователей
     *
     * @param users - список
     */
    public void updateUsersList(List<String> users) {
        // Удаляем из списка текущего пользователя
        users.remove(nickname);
        // Добавляем строку 'отправить всем'
        users.add(0, "All");
        clientChat.updateUsers(users);
    }

    /**
     * Обновление таймера времени авторизации
     *
     * @param message - оставшееся время в секундах
     */
    public void updateTimeoutLabel(String message) {
        authDialog.updateTimeOutAuthLabel(message);
    }
}
