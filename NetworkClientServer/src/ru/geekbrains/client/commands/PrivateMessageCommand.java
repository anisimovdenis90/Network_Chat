package ru.geekbrains.client.commands;

import java.io.Serializable;

public class PrivateMessageCommand implements Serializable {

    private final String receiver;
    private final String message;
    private final String sender;

    public PrivateMessageCommand(String receiver, String message, String sender) {
        this.receiver = receiver;
        this.message = message;
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }
}