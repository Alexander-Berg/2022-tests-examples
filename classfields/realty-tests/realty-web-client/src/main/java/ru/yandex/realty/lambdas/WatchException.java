package ru.yandex.realty.lambdas;

import org.awaitility.core.ConditionTimeoutException;

@FunctionalInterface
public interface WatchException {
    void execute();

    static void watchException(WatchException method) {
        try {
            method.execute();
        } catch (ConditionTimeoutException e) {
            if (e.getCause() instanceof AssertionError) {
                throw new AssertionError(e.getCause());
            } else {
                throw new AssertionError(e.getMessage());
            }
        }
    }
}
