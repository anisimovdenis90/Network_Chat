package ru.geekbrains.client.view;

import ru.geekbrains.client.controller.ClientController;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ClientChat extends JFrame {

    private final ClientController controller;

    private JPanel mainPanel;
    private JList<String> usersList;
    private JTextField messageTextField;
    private JButton sendButton;
    private JTextArea chatText;
    private JTextField nicknameText;
    private JButton changeNickButton;

    private static final String newNickLengthErrorMessage = "Неверное или слишком короткое имя пользователя.";

    public ClientChat(ClientController controller) {
        this.controller = controller;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);
        setContentPane(mainPanel);
        addListeners();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                controller.sendEndMessage();
                controller.shutdown();
            }
        });
    }

    private void addListeners() {
        sendButton.addActionListener(e -> ClientChat.this.sendMessage());
        messageTextField.addActionListener(e -> sendMessage());
        changeNickButton.addActionListener(e -> ClientChat.this.sendChangeNicknameMessage());
    }

    private void sendChangeNicknameMessage() {
        final String newNickname = nicknameText.getText().trim();
        if (newNickname.isEmpty()) {
            nicknameText.setText(null);
            return;
        } else if (newNickname.length() < 3 || controller.getUsername().equals(newNickname)) {
            showError(newNickLengthErrorMessage);
            return;
        }
        controller.sendChangeNickNameMessage(newNickname);
        nicknameText.setText(null);
    }

    private void sendMessage() {
        final String message = messageTextField.getText().trim();
        if (message.isEmpty()) {
            messageTextField.setText(null);
            return;
        }

        if (usersList.getSelectedIndex() < 1) {
            appendMessage("Я: " + message);
            controller.sendMessageToAllUsers(message);
        } else {
            String username = usersList.getSelectedValue();
            appendMessage(String.format("Я лично %s: %s", username, message));
            controller.sendPrivateMessage(username, message, controller.getUsername());
        }

        messageTextField.setText(null);
    }

    public void appendMessage(String message) {
        controller.getChatHistory().writeHistory(message);
        updateChatText(message);
    }

    public void updateChatText(String message) {
        SwingUtilities.invokeLater(() -> {
            chatText.append(message + System.lineSeparator());
            chatText.setCaretPosition(chatText.getDocument().getLength());
        });
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка!", JOptionPane.ERROR_MESSAGE);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void updateUsers(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            model.addAll(users);
            usersList.setModel(model);
        });
    }

    public void updateTitle(String nickname) {
        SwingUtilities.invokeLater(() -> ClientChat.this.setTitle(nickname));
    }
}
