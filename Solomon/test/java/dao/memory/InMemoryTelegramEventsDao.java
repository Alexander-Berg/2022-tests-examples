package ru.yandex.solomon.alert.dao.memory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.yandex.solomon.alert.dao.TelegramEventRecord;
import ru.yandex.solomon.alert.dao.TelegramEventsDao;


/**
 * @author alexlovkov
 **/
public class InMemoryTelegramEventsDao implements TelegramEventsDao {

    private final ConcurrentMap<String, TelegramEventRecord> dao;

    public InMemoryTelegramEventsDao() {
        this.dao = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Void> insert(TelegramEventRecord record) {
        return CompletableFuture.runAsync(() -> dao.put(record.getId(), record));
    }

    @Override
    public CompletableFuture<Optional<TelegramEventRecord>> find(String id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(dao.get(id)));
    }

    @Override
    public CompletableFuture<Void> deleteOlderThan(Instant instant) {
        return CompletableFuture.runAsync(() -> dao.entrySet()
            .removeIf(entry -> entry.getValue().getCreatedAt() < instant.toEpochMilli()));
    }

    @Override
    public CompletableFuture<?> createSchemaForTests() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> updateContext(TelegramEventRecord record) {
        return CompletableFuture.runAsync(() -> {
            var ctx = dao.get(record.getId()).getContext();
            ctx.copyFrom(record.getContext());
        });
    }

    public Map<String, TelegramEventRecord> getDao() {
        return dao;
    }
}
