package ru.geekbrains.client.view;

import ru.geekbrains.client.controller.ClientController;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class AuthDialog extends JFrame {

    private final ClientController controller;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField loginText;
    private JPasswordField passwordText;
    private JLabel timeOutAuth;


    public AuthDialog(ClientController controller) {
        this.controller = controller;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        setSize(400, 250);
        setLocationRelativeTo(null);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private void onOK() {
        final String login = loginText.getText().trim();
        final String pass = new String(passwordText.getPassword()).trim();
        try {
            controller.sendAuthMessage(login, pass);
        } catch (IOException e) {
            showError("Ошибка при попытке аутентификации");
        }
    }

    private void onCancel() {
        System.exit(0);
    }

    public void showError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage, "Ошибка!", JOptionPane.ERROR_MESSAGE);
    }

    public void showErrorAndClose(String errorMessage) {
        showError(errorMessage);
        onCancel();
    }

    public void updateTimeOutAuthLabel(String seconds) {
        SwingUtilities.invokeLater(() -> {
            if (!timeOutAuth.isEnabled()) {
                timeOutAuth.setEnabled(true);
            }
            timeOutAuth.setText("Лимит времени авторизации: " + seconds + " сек.");
        });
    }
}
