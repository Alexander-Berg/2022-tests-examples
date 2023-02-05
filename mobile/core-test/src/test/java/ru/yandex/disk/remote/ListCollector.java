package ru.yandex.disk.remote;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ListCollector<T> extends ArrayList<T> implements RemoteRepoOnNext<List<T>, RuntimeException> {
    @Override
    public void onNext(@Nonnull final List<T> element) {
        addAll(element);
    }
}