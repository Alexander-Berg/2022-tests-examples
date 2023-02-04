package ru.yandex.solomon.alert.cluster.balancer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.solomon.alert.cluster.broker.AlertingProjectShard;
import ru.yandex.solomon.alert.protobuf.TAssignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TAssignProjectResponse;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectResponse;
import ru.yandex.solomon.balancer.AssignmentSeqNo;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertingLocalShardsStub implements AlertingLocalShards {
    private final ConcurrentMap<String, AlertingProjectShard> shards = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> readinessByShardId = new ConcurrentHashMap<>();

    @Override
    public boolean isAssignmentActual(AssignmentSeqNo seqNo) {
        return true;
    }

    @Override
    public CompletableFuture<TAssignProjectResponse> assignShard(TAssignProjectRequest request) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("not implemented"));
    }

    @Override
    public CompletableFuture<TUnassignProjectResponse> unassignShard(TUnassignProjectRequest request) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("not implemented"));
    }

    @Nullable
    @Override
    public AlertingProjectShard getShardById(String shardId) {
        return null;
    }

    @Override
    public boolean isReady(String shardId) {
        return readinessByShardId.getOrDefault(shardId, Boolean.FALSE);
    }

    public void addShard(String shardId, AlertingProjectShard shard) {
        shards.put(shardId, shard);
    }

    public void setReady(String shardId, boolean value) {
        readinessByShardId.put(shardId, value);
    }

    @Override
    @Nonnull
    public Iterator<AlertingProjectShard> iterator() {
        return shards.values().iterator();
    }

    @Override
    public List<String> assignedShards() {
        return new ArrayList<>(shards.keySet());
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<Void> gracefulShutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Stream<AlertingProjectShard> stream() {
        return shards.values().stream();
    }
}
