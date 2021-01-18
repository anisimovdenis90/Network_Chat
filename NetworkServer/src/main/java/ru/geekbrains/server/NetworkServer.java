package ru.geekbrains.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.geekbrains.client.Command;
import ru.geekbrains.server.client.ClientHandler;
import ru.geekbrains.server.dbservices.DBConnector;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkServer {

    private static final Logger infoLogger = LogManager.getLogger(NetworkServer.class.getName());
    private static final Logger fatalLogger = LogManager.getRootLogger();

    private final DBConnector dbConnector;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;

    private final int port;

    private final boolean isCensorEnable;

    public NetworkServer(int port, boolean enableCensor) {
        this.port = port;
        this.isCensorEnable = enableCensor;
        this.executor = Executors.newCachedThreadPool();
        this.dbConnector = new DBConnector();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public static Logger getInfoLogger() {
        return infoLogger;
    }

    public static Logger getFatalLogger() {
        return fatalLogger;
    }

    public void start() {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            infoLogger.info("Сервер был успешно запущен на порту " + port);
            while (true) {
                infoLogger.info("Ожидание подключения клиента...");
                final Socket clientSocket = serverSocket.accept();
                infoLogger.info("Клиент подключился");
                createClientHandler(clientSocket, dbConnector);
            }
        } catch (IOException e) {
            fatalLogger.error("Ошибка при работе сервера", e);
        } finally {
            executor.shutdown();
        }
    }

    private void createClientHandler(Socket clientSocket, DBConnector dbConnector) {
        executor.execute(new ClientHandler(this, clientSocket, dbConnector, isCensorEnable));
    }

    public void broadcastMessage(Command message, ClientHandler owner) throws IOException {
        for (final ClientHandler client : clients) {
            if (client != owner) {
                client.sendMessage(message);
            }
        }
    }

    public void sendPrivateMessage(String receiver, Command command) throws IOException {
        for (final ClientHandler client : clients) {
            if (client.getNickname().equals(receiver)) {
                client.sendMessage(command);
                break;
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) throws IOException {
        clients.add(clientHandler);
        final List<String> users = getAllUsernames();
        broadcastMessage(Command.updateUsersListCommand(users), null);
    }

    public void unsubscribe(ClientHandler clientHandler) throws IOException {
        clients.remove(clientHandler);
        final List<String> users = getAllUsernames();
        broadcastMessage(Command.updateUsersListCommand(users), null);
    }

    private List<String> getAllUsernames() {
        final List<String> usernames = new LinkedList<>();
        for (final ClientHandler client : clients) {
            usernames.add(client.getNickname());
        }
        return usernames;
    }

    public boolean isNicknameBusy(String username) {
        for (final ClientHandler client : clients) {
            if (client.getNickname().equals(username)) {
                return true;
            }
        }
        return false;
    }
}
