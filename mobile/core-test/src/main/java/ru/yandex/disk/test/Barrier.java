package ru.yandex.disk.test;

public class Barrier {

    static final int TIMEOUT_IN_MILLIS = 5000;

    public synchronized void block() {
        try {
            wait(TIMEOUT_IN_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void unblock() {
        notify();
    }

}