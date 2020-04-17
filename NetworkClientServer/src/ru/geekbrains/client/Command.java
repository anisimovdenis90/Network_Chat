package ru.geekbrains.client;

import ru.geekbrains.client.commands.*;

import java.io.Serializable;
import java.util.List;

public class Command implements Serializable {

    private CommandType type;
    private Object data;

    public Object getData() {
        return data;
    }

    public CommandType getType() {
        return type;
    }

    public static Command authCommand(String login, String password) {
        Command command = new Command();
        command.type = CommandType.AUTH;
        command.data = new AuthCommand(login, password);
        return command;
    }

    public static Command authErrorCommand(String errorMessage) {
        Command command = new Command();
        command.type = CommandType.AUTH_ERROR;
        command.data = new ErrorCommand(errorMessage);
        return command;
    }

    public static Command timeoutAuthErrorCommand(String errorMessage) {
        Command command = new Command();
        command.type = CommandType.TIMEOUT_AUTH_ERROR;
        command.data = new ErrorCommand(errorMessage);
        return command;
    }

    public static Command errorCommand(String errorMessage) {
        Command command = new Command();
        command.type = CommandType.ERROR;
        command.data = new ErrorCommand(errorMessage);
        return command;
    }

    public static Command messageCommand(String username, String message) {
        Command command = new Command();
        command.type = CommandType.MESSAGE;
        command.data = new MessageCommand(username, message);
        return command;
    }

    public static Command changeNicknameMessageCommand(String oldNickname, String newNickname) {
        Command command = new Command();
        command.type = CommandType.CHANGE_NICKNAME_MESSAGE;
        command.data = new MessageCommand(oldNickname, newNickname);
        return command;
    }

    public static Command timeoutAuthMessageCommand(String message) {
        Command command = new Command();
        command.type = CommandType.TIMEOUT_MESSAGE;
        command.data = new BroadcastMessageCommand(message);
        return command;
    }

    public static Command broadcastMessageCommand(String message) {
        Command command = new Command();
        command.type = CommandType.BROADCAST_MESSAGE;
        command.data = new BroadcastMessageCommand(message);
        return command;
    }

    public static Command privateMessageCommand(String receiver, String message, String sender) {
        Command command = new Command();
        command.type = CommandType.PRIVATE_MESSAGE;
        command.data = new PrivateMessageCommand(receiver, message, sender);
        return command;
    }

    public static Command updateUsersListCommand(List<String> users) {
        Command command = new Command();
        command.type = CommandType.UPDATE_USERS_LIST;
        command.data = new UpdateUsersListCommand(users);
        return command;
    }

    public static Command endCommand() {
        Command command = new Command();
        command.type = CommandType.END;
        return command;
    }
}
