package ru.geekbrains.server.client;

import ru.geekbrains.client.Command;
import ru.geekbrains.client.CommandType;
import ru.geekbrains.client.commands.AuthCommand;
import ru.geekbrains.client.commands.BroadcastMessageCommand;
import ru.geekbrains.client.commands.MessageCommand;
import ru.geekbrains.client.commands.PrivateMessageCommand;
import ru.geekbrains.server.NetworkServer;
import ru.geekbrains.server.dbservices.DBConnector;
import ru.geekbrains.server.dbservices.auth.AuthService;
import ru.geekbrains.server.dbservices.auth.Authenticator;
import ru.geekbrains.server.dbservices.censor.Censor;
import ru.geekbrains.server.dbservices.censor.CensorService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final NetworkServer networkServer;
    private final Socket clientSocket;
    private final Authenticator authenticator;
    private Censor censor = null;

    public boolean successfulAuth = false;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String nickname;

    private String errorTimeoutAuthMessage = "Истекло время авторизации, соединение закрыто!";
    private String notFindNickname = "Отсутствует учетная запись по данному логину и паролю!";
    private String userAlreadyOnline = "Данный пользователь уже авторизован!";
    private String nicknameAlreadyUsed = "Введенное имя пользователя уже используется.";
    private String changeNicknameMessage = "Ваше имя успешно изменено на ";
    private String notCensuredNickname = "Недопустимое имя!";

    public ClientHandler(NetworkServer networkServer, Socket socket, DBConnector dbConnector, boolean enableCensor) {
        this.networkServer = networkServer;
        this.clientSocket = socket;
        this.authenticator = new AuthService(dbConnector);
        if (enableCensor) {
            censor = new CensorService(dbConnector);
        }
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void run() {
        startHandler();
    }

    /**
     * Метод организации подключения клиента к серверу
     */
    private void startHandler() {
        try {
            // Создаем потоки обмена информацией
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            // Запускаем в отдельном потоке чтение сообщений от клиента
            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
//                    System.out.println(String.format("Соединение с клиентом %s закрыто!", nickname));
                    NetworkServer.getFatalLogger().fatal("Соединение с клиентом закрыто!");
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
//            e.printStackTrace();
            NetworkServer.getFatalLogger().fatal(String.format("Ошибка работы обработчика клиента %s", nickname), e);
        }
    }

    /**
     * Закрытие соединения с сервером
     */
    private void closeConnection() {
        try {
            // Передаем экземпляр подключения конкретного клиента
            networkServer.unsubscribe(this);
            clientSocket.close();
        } catch (IOException e) {
//            e.printStackTrace();
            NetworkServer.getFatalLogger().fatal(String.format("Ошибка при закрытии соединения с клиентом %s", nickname), e);
        }
    }

    /**
     * Авторизация контакта на сервере
     *
     * @throws IOException - пробрасываем исключение метода readUTF
     */
    private void authentication() throws IOException {
        runTimeOutAuthThread();
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }
            if (command.getType() == CommandType.AUTH) {
                successfulAuth = processAuthCommand(command);
                if (successfulAuth) {
                    return;
                }
            } else {
//                System.err.println("Неверный тип команды процесса авторизации: " + command.getType());
                NetworkServer.getInfoLogger().error("Неверный тип команды процесса авторизации: " + command.getType());
            }
        }
    }

    /**
     * Запускает поток лимита времени авторизации клиента
     */
    private void runTimeOutAuthThread() {
//        System.out.println("Ожидание авторизации клиента...");
        NetworkServer.getInfoLogger().info("Ожидание авторизации клиента...");
        networkServer.getExecutor().execute(() -> {
            try {
//                 Отправляет время в окно авторизации
                for (int i = 120; i > 0; i--) {
                    Command timeoutAuthMessageCommand = Command.timeoutAuthMessageCommand("" + i);
                    ClientHandler.this.sendMessage(timeoutAuthMessageCommand);
                    Thread.sleep(1_000);
                    // Заранее выходим из цикла при успешной авторизации
                    if (successfulAuth)
                        break;
                }
                // Если клиент не авторизовался, закрывается соединение
                if (!successfulAuth) {
//                    System.out.println("Истекло время авторизации, клиент отключен");
                    NetworkServer.getInfoLogger().info("Истекло время авторизации, клиент отключен");
                    Command timeOutAuthErrorCommand = Command.timeoutAuthErrorCommand(errorTimeoutAuthMessage);
                    ClientHandler.this.sendMessage(timeOutAuthErrorCommand);
                    ClientHandler.this.closeConnection();
                }
            } catch (InterruptedException e) {
//                e.printStackTrace();
                NetworkServer.getFatalLogger().fatal("Ошибка потока таймаута авторизации!", e);
            } catch (IOException e) {
//                System.out.println("Авторизация не выполнена, закрыто соединение с клиентом");
                NetworkServer.getFatalLogger().fatal("Авторизация не выполнена, закрыто соединение с клиентом");
            }
        });
    }

    /**
     * Обработка команды авторизации
     *
     * @param command - команда авторизации
     * @return boolean     - true, если получен никнейм
     * @throws IOException - пробрасывается исключение
     */
    private boolean processAuthCommand(Command command) throws IOException {
        AuthCommand commandData = (AuthCommand) command.getData();
        // Получаем из сообщения логин и пароль
        String login = commandData.getLogin();
        String password = commandData.getPassword();
        // Получаем никнейм авторизованного пользователя
        String userID = authenticator.getUserNickAndIDByLoginAndPassword(login, password)[0];
        String username = authenticator.getUserNickAndIDByLoginAndPassword(login, password)[1];
        // Если никнейм отсутствует
        if (username == null) {
            Command authErrorCommand = Command.authErrorCommand(notFindNickname);
            sendMessage(authErrorCommand);
            return false;
        }
        // Если пользователь уже в сети
        else if (networkServer.isNicknameBusy(username)) {
            Command authErrorCommand = Command.authErrorCommand(userAlreadyOnline);
            sendMessage(authErrorCommand);
            return false;
        } else {
            nickname = username;
//            System.out.println(String.format("Клиент %s авторизовался", nickname));
            NetworkServer.getInfoLogger().info(String.format("Клиент %s авторизовался", nickname));
            String message = nickname + " зашел в чат!";
            networkServer.broadcastMessage(Command.messageCommand(null, message), this);
            // Отправляем отклик авторизации клиенту
            commandData.setUserID(userID);
            commandData.setUsername(nickname);
            sendMessage(command);
            networkServer.subscribe(this);
            return true;
        }
    }

    /**
     * Чтение сообщения от клиента
     *
     * @throws IOException - пробрасываем исключение метода readUTF
     */
    private void readMessages() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }
            switch (command.getType()) {
                case END: {
                    String message = nickname + " вышел из чата!";
//                    System.out.println(message);
                    NetworkServer.getInfoLogger().info(message);
                    networkServer.broadcastMessage(Command.messageCommand(null, message), this);
                    return;
                }
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommand commandData = (PrivateMessageCommand) command.getData();
                    String receiver = commandData.getReceiver();
                    String message = commandData.getMessage();
                    message = censor.messageCensor(message);
                    networkServer.sendPrivateMessage(receiver, Command.privateMessageCommand(receiver, message, nickname));
                    break;
                }
                case BROADCAST_MESSAGE: {
                    BroadcastMessageCommand commandData = (BroadcastMessageCommand) command.getData();
                    String message = commandData.getMessage();
                    message = censor.messageCensor(message);
                    networkServer.broadcastMessage(Command.messageCommand(nickname, message), this);
                    break;
                }
                case CHANGE_NICKNAME_MESSAGE: {
                    MessageCommand commandData = (MessageCommand) command.getData();
                    String oldNickname = commandData.getUsername();
                    String newNickname = commandData.getMessage();
                    changeNicknameCommandProcessing(oldNickname, newNickname);
                    break;
                }
                default:
//                    System.err.println("Неверный тип команды: " + command.getType());
                    NetworkServer.getInfoLogger().error("Неверный тип команды: " + command.getType());
            }
        }
    }

    /**
     * Обрабатывает команду на смену никнейма
     *
     * @param oldNickname  - старый никнейм
     * @param newNickname  - новый никнейм
     * @throws IOException - пробрасывается исключение
     */
    private void changeNicknameCommandProcessing(String oldNickname, String newNickname) throws IOException {
        if (!censor.isCensured(newNickname)) {
            Command errorCommand = Command.errorCommand(notCensuredNickname);
            sendMessage(errorCommand);
            return;
        }
        int result = authenticator.changeNickname(oldNickname, newNickname);
        if (result < 1) {
            Command errorCommand = Command.errorCommand(nicknameAlreadyUsed);
            sendMessage(errorCommand);
        } else {
            networkServer.unsubscribe(this);
            nickname = newNickname;
            String broadcastMessage = String.format("%s сменил имя на %s!", oldNickname, nickname);
//            System.out.println(broadcastMessage);
            NetworkServer.getInfoLogger().info(broadcastMessage);
            networkServer.broadcastMessage(Command.messageCommand(null, broadcastMessage), this);
            String message = String.format(changeNicknameMessage + "'%s'!", newNickname);
            Command changeNickNameMessageCommand = Command.changeNicknameMessageCommand(newNickname, message);
            sendMessage(changeNickNameMessageCommand);
            networkServer.subscribe(this);
        }
    }

    /**
     * Чтение данных, полученных от клиента
     *
     * @return - возвращает полученную команду, или null в случае ошибки
     * @throws IOException - пробрасывается исключение
     */
    private Command readCommand() throws IOException {
        try {
            return (Command) in.readObject();
        } catch (ClassNotFoundException e) {
            String errorMessage = "Неизвестный тип объекта от клиента";
//            System.err.println(errorMessage);
            NetworkServer.getInfoLogger().error(errorMessage);
            e.printStackTrace();
            sendMessage(Command.errorCommand(errorMessage));
            return null;
        }
    }

    /**
     * Отправляет сообщение клиенту
     *
     * @param command - объект, содержащий сообщение
     * @throws IOException - пробрасывается исключение
     */
    public void sendMessage(Command command) throws IOException {
        out.writeObject(command);
    }
}
