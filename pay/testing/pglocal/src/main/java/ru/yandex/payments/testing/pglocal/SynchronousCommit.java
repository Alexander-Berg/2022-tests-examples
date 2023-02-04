package ru.yandex.payments.testing.pglocal;

public enum SynchronousCommit {
    ON,
    OFF,
    REMOTE_APPLY,
    REMOTE_WRITE;

    public String configValue() {
        return name().toLowerCase();
    }
}
