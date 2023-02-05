package ru.yandex.navi.tf;

public class NoRetryException extends AssertionError {
    public NoRetryException(String message) {
        super(message);
    }
}
