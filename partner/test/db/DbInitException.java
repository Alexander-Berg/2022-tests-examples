package ru.yandex.partner.test.db;

public class DbInitException extends RuntimeException {
    public DbInitException(String msg, InterruptedException exc) {
        super(msg, exc);
    }
}
