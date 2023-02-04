package ru.yandex.solomon.alert.cluster.balancer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.yandex.solomon.alert.dao.ShardsDao;
import ru.yandex.solomon.balancer.ShardsHolder;

/**
 * @author Vladimir Gordiychuk
 */
public class ShardsHolderStub implements ShardsHolder {
    private final ShardsDao dao;
    public final ConcurrentMap<String, String> shards = new ConcurrentHashMap<>();

    public ShardsHolderStub(ShardsDao dao) {
        this.dao = dao;
    }

    @Override
    public CompletableFuture<Void> reload() {
        return dao.findAll().thenAccept(ids -> {
            for (String id : ids) {
                shards.put(id, id);
            }
            System.out.println("Reload(Shards): " + shards.keySet());
        });
    }

    @Override
    public Set<String> getShards() {
        return shards.keySet();
    }

    @Override
    public CompletableFuture<Void> add(String shardId) {
        return dao.insert(shardId).thenRun(() -> shards.put(shardId, shardId));
    }

    @Override
    public CompletableFuture<Void> delete(String shardId) {
        return dao.delete(shardId).thenRun(() -> shards.remove(shardId));
    }
}
