package ru.yandex.solomon.alert.dao.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import ru.yandex.solomon.alert.dao.TelegramDao;
import ru.yandex.solomon.alert.dao.TelegramRecord;


/**
 * @author alexlovkov
 **/
public class InMemoryTelegramDao implements TelegramDao {

    private final Map<Long, TelegramRecord> map;
    private CountDownLatch countDownLatch;

    public InMemoryTelegramDao() {
        this(1);
    }

    public InMemoryTelegramDao(int count) {
        this.map = new ConcurrentHashMap<>();
        this.countDownLatch = new CountDownLatch(count);
    }

    @Override
    public CompletableFuture<Void> migrate() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> upsert(TelegramRecord record) {
        return CompletableFuture.supplyAsync(() -> {
            map.put(record.getChatId(), record);
            countDownLatch.countDown();
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<TelegramRecord>> get(long chatId) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(map.get(chatId)));
    }

    @Override
    public CompletableFuture<Void> deleteById(long chatId) {
        return CompletableFuture.supplyAsync(() -> {
            map.remove(chatId);
            countDownLatch.countDown();
            return null;
        });
    }

    @Override
    public CompletableFuture<List<TelegramRecord>> findAll() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(map.values()));
    }

    @Override
    public CompletableFuture<?> createSchemaForTests() {
        return CompletableFuture.supplyAsync(() -> null);
    }

    public Map<Long, TelegramRecord> getMap() {
        return map;
    }

    public void setCountDownLatch(int count) {
        this.countDownLatch = new CountDownLatch(count);
    }

    public void sync() throws InterruptedException {
        countDownLatch.await();
    }
}
