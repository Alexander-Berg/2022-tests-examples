package ru.yandex.payments.tvmlocal.testing.exception;

public class TvmToolLaunchException extends RuntimeException {
    public TvmToolLaunchException() {
        super("tvmtool launch failed");
    }
}
