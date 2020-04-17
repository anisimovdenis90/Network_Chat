package ru.geekbrains.client.controller;

@FunctionalInterface
public interface AuthEvent {
    void authIsSuccessful(String nickname, String userID);
}
