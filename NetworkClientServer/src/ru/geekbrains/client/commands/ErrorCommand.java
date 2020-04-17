package ru.geekbrains.client.commands;

import java.io.Serializable;

public class ErrorCommand implements Serializable {

    private final String errorMessage;

    public ErrorCommand(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
