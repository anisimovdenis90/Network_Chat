package ru.geekbrains.server.dbservices.censor;

public interface Censor {

    String messageCensor(String message);

    boolean isCensured(String message);
}
