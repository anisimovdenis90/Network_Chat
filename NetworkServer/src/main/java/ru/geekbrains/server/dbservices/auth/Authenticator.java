package ru.geekbrains.server.dbservices.auth;

public interface Authenticator {

    String[] getUserNickAndIDByLoginAndPassword(String login, String password);

    int changeNickname(String oldNickname, String newNickname);

}
