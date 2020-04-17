package ru.geekbrains.client.commands;

import java.io.Serializable;

public class MessageCommand implements Serializable {

    private final String username;
    private final String message;

    public MessageCommand(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }
}
