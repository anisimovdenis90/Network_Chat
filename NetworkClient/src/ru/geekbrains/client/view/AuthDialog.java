package ru.geekbrains.client.view;

import ru.geekbrains.client.controller.ClientController;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class AuthDialog extends JFrame {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField loginText;
    private JPasswordField passwordText;
    private JLabel timeOutAuth;

    private ClientController controller;

    public AuthDialog(ClientController controller) {
        this.controller = controller;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        setSize(400, 250);
        setLocationRelativeTo(null);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        // Действие при закрытии окна
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    /**
     * Действие на кнопку Ok
     */
    private void onOK() {
        // Получаем строки с логином и паролем из полей ввода
        String login = loginText.getText().trim();
        String pass = new String(passwordText.getPassword()).trim();
        try {
            // отправляем сообщение авторизации серверу
            controller.sendAuthMessage(login, pass);
        } catch (IOException e) {
            showError("Ошибка при попытке аутентификации");
        }
    }

    /**
     * Действие на кнопку Cancel
     */
    private void onCancel() {
        System.exit(0);
    }

    /**
     * Отображение окна с описанием ошибки
     * @param errorMessage - текст ошибки
     */
    public void showError(String errorMessage) {
        JOptionPane.showMessageDialog(this, errorMessage, "Ошибка!", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Отображает ошибку и закрывает клиент
     * @param errorMessage - текст ошибки
     */
    public void showErrorAndClose(String errorMessage) {
        showError(errorMessage);
        onCancel();
    }

    /**
     * Таймер времени авторизации клиента
     * @param seconds - остаток времени в секундах
     */
    public void updateTimeOutAuthLabel(String seconds) {
        SwingUtilities.invokeLater(() -> {
            if (!timeOutAuth.isEnabled()) {
                timeOutAuth.setEnabled(true);
            }
            timeOutAuth.setText("Лимит времени авторизации: " + seconds + " сек.");
        });
    }
}
