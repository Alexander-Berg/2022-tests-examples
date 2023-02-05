package ru.yandex.disk.remote;

import javax.annotation.Nonnull;

import java.util.ArrayList;

public class Collector<T> extends ArrayList<T> implements RemoteRepoOnNext<T, RuntimeException> {
    @Override
    public void onNext(@Nonnull final T element) throws RuntimeException {
        add(element);
    }
}