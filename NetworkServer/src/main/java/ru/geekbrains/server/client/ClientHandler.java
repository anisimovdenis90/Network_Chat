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

    private static final String errorTimeoutAuthMessage = "Истекло время авторизации, соединение закрыто!";
    private static final String notFindNickname = "Отсутствует учетная запись по данному логину и паролю!";
    private static final String userAlreadyOnline = "Данный пользователь уже авторизован!";
    private static final String nicknameAlreadyUsed = "Введенное имя пользователя уже используется.";
    private static final String changeNicknameMessage = "Ваше имя успешно изменено на ";
    private static final String notCensuredNickname = "Недопустимое имя!";

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

    private void startHandler() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

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

    private void closeConnection() {
        try {
            networkServer.unsubscribe(this);
            clientSocket.close();
        } catch (IOException e) {
//            e.printStackTrace();
            NetworkServer.getFatalLogger().fatal(String.format("Ошибка при закрытии соединения с клиентом %s", nickname), e);
        }
    }

    private void authentication() throws IOException {
        runTimeOutAuthThread();
        while (true) {
            final Command command = readCommand();
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

    private void runTimeOutAuthThread() {
//        System.out.println("Ожидание авторизации клиента...");
        NetworkServer.getInfoLogger().info("Ожидание авторизации клиента...");
        networkServer.getExecutor().execute(() -> {
            try {
                for (int i = 120; i > 0; i--) {
                    final Command timeoutAuthMessageCommand = Command.timeoutAuthMessageCommand("" + i);
                    sendMessage(timeoutAuthMessageCommand);
                    Thread.sleep(1_000);

                    if (successfulAuth)
                        break;
                }

                if (!successfulAuth) {
//                    System.out.println("Истекло время авторизации, клиент отключен");
                    NetworkServer.getInfoLogger().info("Истекло время авторизации, клиент отключен");
                    final Command timeOutAuthErrorCommand = Command.timeoutAuthErrorCommand(errorTimeoutAuthMessage);
                    sendMessage(timeOutAuthErrorCommand);
                    closeConnection();
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

    private boolean processAuthCommand(Command command) throws IOException {
        final AuthCommand commandData = (AuthCommand) command.getData();
        final String login = commandData.getLogin();
        final String password = commandData.getPassword();
        final String userID = authenticator.getUserNickAndIDByLoginAndPassword(login, password)[0];
        final String username = authenticator.getUserNickAndIDByLoginAndPassword(login, password)[1];

        if (username == null) {
            final Command authErrorCommand = Command.authErrorCommand(notFindNickname);
            sendMessage(authErrorCommand);
            return false;
        } else if (networkServer.isNicknameBusy(username)) {
            final Command authErrorCommand = Command.authErrorCommand(userAlreadyOnline);
            sendMessage(authErrorCommand);
            return false;
        } else {
            nickname = username;
//            System.out.println(String.format("Клиент %s авторизовался", nickname));
            NetworkServer.getInfoLogger().info(String.format("Клиент %s авторизовался", nickname));
            final String message = nickname + " зашел в чат!";
            networkServer.broadcastMessage(Command.messageCommand(null, message), this);
            commandData.setUserID(userID);
            commandData.setUsername(nickname);
            sendMessage(command);
            networkServer.subscribe(this);
            return true;
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            final Command command = readCommand();
            if (command == null) {
                continue;
            }
            switch (command.getType()) {
                case END: {
                    final String message = nickname + " вышел из чата!";
//                    System.out.println(message);
                    NetworkServer.getInfoLogger().info(message);
                    networkServer.broadcastMessage(Command.messageCommand(null, message), this);
                    return;
                }
                case PRIVATE_MESSAGE: {
                    final PrivateMessageCommand commandData = (PrivateMessageCommand) command.getData();
                    final String receiver = commandData.getReceiver();
                    String message = commandData.getMessage();
                    message = censor.messageCensor(message);
                    networkServer.sendPrivateMessage(receiver, Command.privateMessageCommand(receiver, message, nickname));
                    break;
                }
                case BROADCAST_MESSAGE: {
                    final BroadcastMessageCommand commandData = (BroadcastMessageCommand) command.getData();
                    String message = commandData.getMessage();
                    message = censor.messageCensor(message);
                    networkServer.broadcastMessage(Command.messageCommand(nickname, message), this);
                    break;
                }
                case CHANGE_NICKNAME_MESSAGE: {
                    final MessageCommand commandData = (MessageCommand) command.getData();
                    final String oldNickname = commandData.getUsername();
                    final String newNickname = commandData.getMessage();
                    changeNicknameCommandProcessing(oldNickname, newNickname);
                    break;
                }
                default:
//                    System.err.println("Неверный тип команды: " + command.getType());
                    NetworkServer.getInfoLogger().error("Неверный тип команды: " + command.getType());
            }
        }
    }

    private void changeNicknameCommandProcessing(String oldNickname, String newNickname) throws IOException {
        if (!censor.isCensured(newNickname)) {
            final Command errorCommand = Command.errorCommand(notCensuredNickname);
            sendMessage(errorCommand);
            return;
        }
        int result = authenticator.changeNickname(oldNickname, newNickname);
        if (result < 1) {
            final Command errorCommand = Command.errorCommand(nicknameAlreadyUsed);
            sendMessage(errorCommand);
        } else {
            networkServer.unsubscribe(this);
            nickname = newNickname;
            final String broadcastMessage = String.format("%s сменил имя на %s!", oldNickname, nickname);
//            System.out.println(broadcastMessage);
            NetworkServer.getInfoLogger().info(broadcastMessage);
            networkServer.broadcastMessage(Command.messageCommand(null, broadcastMessage), this);
            final String message = String.format(changeNicknameMessage + "'%s'!", newNickname);
            final Command changeNickNameMessageCommand = Command.changeNicknameMessageCommand(newNickname, message);
            sendMessage(changeNickNameMessageCommand);
            networkServer.subscribe(this);
        }
    }

    private Command readCommand() throws IOException {
        try {
            return (Command) in.readObject();
        } catch (ClassNotFoundException e) {
            final String errorMessage = "Неизвестный тип объекта от клиента";
//            System.err.println(errorMessage);
            NetworkServer.getInfoLogger().error(errorMessage);
            e.printStackTrace();
            sendMessage(Command.errorCommand(errorMessage));
            return null;
        }
    }

    public void sendMessage(Command command) throws IOException {
        out.writeObject(command);
    }
}
