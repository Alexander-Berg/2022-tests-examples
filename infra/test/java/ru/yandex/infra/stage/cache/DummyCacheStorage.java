package ru.yandex.infra.stage.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Message;

public class DummyCacheStorage<TProtoValue extends Message> implements CacheStorage<TProtoValue> {

    public Map<String, TProtoValue> readResult = new HashMap<>();

    public int readCount;
    public int flushCount;
    public int initCount;
    public int writeCount;
    public int removeCount;

    @Override
    public CompletableFuture<?> write(Map<String, TProtoValue> newValues) {
        writeCount++;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> init() {
        initCount++;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> flush() {
        flushCount++;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Map<String, TProtoValue>> read(){
        readCount++;
        return CompletableFuture.completedFuture(readResult);
    }

    @Override
    public CompletableFuture<?> write(String key, TProtoValue tProtoValue) {
        writeCount++;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> remove(String key) {
        removeCount++;
        return CompletableFuture.completedFuture(null);
    }
}

