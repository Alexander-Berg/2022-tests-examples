package ru.yandex.infra.stage.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DummyCache<T> implements Cache<T> {
    public final Map<String, T> map;
    public int putCallsCount;
    public int removeCallsCount;

    public CompletableFuture<?> putCallFuture = new CompletableFuture<>();
    public CompletableFuture<?> putResult = CompletableFuture.completedFuture(null);
    public CompletableFuture<?> removeCallFuture = new CompletableFuture<>();

    public DummyCache(Map<String, T> map) {
        this.map = new HashMap<>(map);
    }

    public DummyCache() {
        map = new HashMap<>();
    }

    @Override
    public Map<String, T> getAll() {
        return map;
    }

    @Override
    public Optional<T> get(String key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public CompletableFuture<?> put(String key, T t) {
        putCallsCount++;
        map.put(key, t);
        putCallFuture.complete(null);
        return putResult;
    }

    @Override
    public CompletableFuture<?> remove(String key) {
        removeCallsCount++;
        map.remove(key);
        removeCallFuture.complete(null);
        return CompletableFuture.completedFuture(null);
    }
}
