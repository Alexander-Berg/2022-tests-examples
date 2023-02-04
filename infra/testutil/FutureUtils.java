package ru.yandex.infra.controller.testutil;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Throwables;

import ru.yandex.infra.controller.util.ExceptionUtils;

public class FutureUtils {
    // Shortcut for common get, avoid checked exceptions and hanging tests in case of problems
    public static <T> T get1s(CompletableFuture<T> future) {
        return getImpl(future, 1);
    }

    public static <T> T get5s(CompletableFuture<T> future) {
        return getImpl(future, 5);
    }

    // Pass timeout as Duration and strip meaningless exceptions
    public static <T> T getUnchecked(CompletableFuture<T> future, Duration timeout) {
        try {
            return future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            Throwable stripped = ExceptionUtils.stripCompletionException(e);
            Throwables.throwIfUnchecked(stripped);
            throw new RuntimeException(stripped);
        }
    }

    private static <T> T getImpl(Future<T> future, int seconds) {
        try {
            return future.get(seconds, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            Throwable t = ExceptionUtils.stripCompletionException(e);
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException(t);
        }
    }
}
