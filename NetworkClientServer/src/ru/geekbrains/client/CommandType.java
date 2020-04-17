package ru.geekbrains.client;

public enum CommandType {

    AUTH,
    AUTH_ERROR,
    TIMEOUT_AUTH_ERROR,
    PRIVATE_MESSAGE,
    BROADCAST_MESSAGE,
    MESSAGE,
    TIMEOUT_MESSAGE,
    CHANGE_NICKNAME_MESSAGE,
    UPDATE_USERS_LIST,
    ERROR,
    END

}
