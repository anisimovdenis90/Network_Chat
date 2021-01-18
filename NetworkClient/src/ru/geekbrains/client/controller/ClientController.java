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
        networkService.setSuccessfulAuthEvent(new AuthEvent() {
            @Override
            public void authIsSuccessful(String nickname, String userID) {
                setUserName(nickname);
                chatHistory = new ChatHistory(ClientController.this, userID);
                chatHistory.readHistory();
                clientChat.setTitle(nickname);
                ClientController.this.openChat();
            }
        });
        authDialog.setVisible(true);
    }

    private void openChat() {
        authDialog.dispose();
        networkService.setMessageHandler(new MessageHandler() {
            @Override
            public void handle(String message) {
                clientChat.appendMessage(message);
            }
        });
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
        if (chatHistory != null) {
            chatHistory.stopWriteChatHistory();
        }
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
        users.remove(nickname);
        users.add(0, "All");
        clientChat.updateUsers(users);
    }

    public void updateTimeoutLabel(String message) {
        authDialog.updateTimeOutAuthLabel(message);
    }
}
