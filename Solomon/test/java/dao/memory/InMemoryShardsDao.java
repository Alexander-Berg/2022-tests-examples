package ru.yandex.solomon.alert.dao.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import ru.yandex.solomon.alert.dao.ShardsDao;


/**
 * @author Vladimir Gordiychuk
 **/
public class InMemoryShardsDao implements ShardsDao {
    private final Map<String, String> projectIdByShardId;
    private volatile Throwable throwable;

    public InMemoryShardsDao() {
        this.projectIdByShardId = new ConcurrentHashMap<>();
    }

    public void setError(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public CompletableFuture<?> createSchemaForTests() {
        return async(() -> {});
    }

    public void addShards(String... ids) {
        for (var id : ids) {
            projectIdByShardId.put(id, id);
        }
    }

    @Override
    public CompletableFuture<Void> insert(String shardId) {
        return async(() -> projectIdByShardId.put(shardId, shardId));
    }

    @Override
    public CompletableFuture<Void> delete(String shardId) {
        return async(() -> projectIdByShardId.remove(shardId));
    }

    @Override
    public CompletableFuture<List<String>> findAll() {
        return CompletableFuture.supplyAsync(() -> List.copyOf(projectIdByShardId.values()));
    }

    private CompletableFuture<Void> async(Runnable runnable) {
        return CompletableFuture.supplyAsync(() -> {
            var copy = throwable;
            if (throwable != null) {
                throw new RuntimeException(throwable);
            }
            runnable.run();
            return null;
        });

    }
}
