package ru.yandex.solomon.alert.cluster.balancer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ru.yandex.solomon.alert.protobuf.TAssignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TAssignProjectResponse;
import ru.yandex.solomon.alert.protobuf.THeartbeatRequest;
import ru.yandex.solomon.alert.protobuf.THeartbeatResponse;
import ru.yandex.solomon.alert.protobuf.TProjectAssignmentRequest;
import ru.yandex.solomon.alert.protobuf.TProjectAssignmentResponse;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectResponse;


/**
 * @author Vladimir Gordiychuk
 */
public abstract class AbstractAlertingNode {
    protected final String name;
    private final AtomicReference<CompletableFuture<?>> messageSync = new AtomicReference<>(new CompletableFuture<>());

    public AbstractAlertingNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public CompletableFuture<TAssignProjectResponse> assignShard(TAssignProjectRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    public CompletableFuture<TUnassignProjectResponse> unassignShard(TUnassignProjectRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    public CompletableFuture<TProjectAssignmentResponse> listAssignments(TProjectAssignmentRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    public CompletableFuture<THeartbeatResponse> heartbeat(THeartbeatRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    public CompletableFuture<?> getMessageSync() {
        return messageSync.get();
    }

    public boolean awaitMessage(long millis) throws InterruptedException {
        CountDownLatch sync = new CountDownLatch(1);
        getMessageSync()
            .whenComplete((o, throwable) -> {
                sync.countDown();
            });
        return sync.await(millis, TimeUnit.MILLISECONDS);
    }

    final void onCompleteMessage() {
        messageSync.getAndSet(new CompletableFuture<>()).complete(null);
    }
}
