package ru.yandex.infra.stage.yp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.Message;

import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.yp.CreateObjectRequest;
import ru.yandex.infra.controller.yp.DummyYpObjectTransactionalRepository;
import ru.yandex.infra.controller.yp.YpObjectTransactionalRepository;
import ru.yandex.infra.stage.dto.ClusterAndType;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

public class DummyObjectLifeCycleManager<Meta extends SchemaMeta, Spec extends Message, Status extends Message>
        implements ObjectLifeCycleManager<Meta, Spec, Status> {
    public Map<String, Boolean> wasCreated = new HashMap<>();
    public Map<String, Boolean> wasUpdated = new HashMap<>();
    public Map<String, Boolean> wasStopped = new HashMap<>();
    public Map<String, Boolean> wasStarted = new HashMap<>();
    public Map<String, String> removedLabels = new HashMap<>();
    public Runnable successCall;
    public Consumer<Throwable> failureCall;
    public Optional<Long> lastMetaTimestamp;

    private Map<String, BiConsumer<Optional<SpecStatusMeta<Meta, Spec, Status>>, ObjectLifeCycleEventType>> currentSubscribers = new HashMap<>();
    private Map<String, Spec> currentSpecs = new HashMap<>();
    private Map<String, Acl> currentAcls = new HashMap<>();
    public Map<String, Map<String, YTreeNode>> currentLabels = new HashMap<>();
    private Map<String, Map<String, YTreeNode>> currentFields = new HashMap<>();
    public final LoadingCache<Integer, String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public String load(Integer key) {
                    return key.toString();
                }
            });
    public final DummyYpObjectTransactionalRepository<Meta, Spec, Status> repository = new DummyYpObjectTransactionalRepository<>();

    @Override
    public void create(String id, CreateObjectRequest<Spec> request, Runnable onSuccess, Consumer<Throwable> onFailure) {
        this.wasCreated.put(id, true);
        this.successCall = onSuccess;
        this.failureCall = onFailure;
        this.currentSpecs.put(id, request.getSpec());
        this.currentAcls.put(id, request.getAcl().get());
        onSuccess.run();
    }

    @Override
    public void create(String id, Function<YpObjectTransactionalRepository<Meta, Spec, Status>, CompletableFuture<?>> createAction, Runnable onSuccess, Consumer<Throwable> onFailure) {
        try {
            createAction.apply(repository).thenAccept(ignore -> {
                var specs = repository.createObjectRequestSpecs;
                var request = specs.get(specs.size()-1);
                create(id, request, onSuccess, onFailure);
            }).get();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isInCache(int hash) {
        return cache.getIfPresent(hash) != null;
    }

    @Override
    public Optional<String> getCluster() {
        return Optional.empty();
    }

    @Override
    public void addError(int hash, String description) {
        cache.put(hash, description);
    }

    @Override
    public void updateSpecWithLabels(String id, Spec newSpec, Map<String, YTreeNode> labels, Optional<Long> timestamp,
                                     Runnable onSuccess, Consumer<Throwable> onFailure) {
        this.wasUpdated.put(id, true);
        this.successCall = onSuccess;
        this.failureCall = onFailure;
        this.currentSpecs.put(id, newSpec);
        this.currentLabels.put(id, labels);
        onSuccess.run();
    }

    @Override
    public void updateAcl(String id, Acl acl, Optional<Long> metaTimestamp, Runnable onSuccess,
            Consumer<Throwable> onFailure) {
        this.wasUpdated.put(id, true);
        this.successCall = onSuccess;
        this.failureCall = onFailure;
        this.currentAcls.put(id, acl);
        this.lastMetaTimestamp = metaTimestamp;
        onSuccess.run();
    }

    @Override
    public void removeLabel(String id, String label, Runnable onSuccess, Consumer<Throwable> onFailure) {
        this.wasUpdated.put(id, true);
        this.removedLabels.put(id, label);
        this.successCall = onSuccess;
        this.failureCall = onFailure;
        onSuccess.run();
    }

    @Override
    public void startManaging(String id, BiConsumer<Optional<SpecStatusMeta<Meta, Spec, Status>>, ObjectLifeCycleEventType> subscriber) {
        this.wasStarted.put(id, true);
        this.currentSubscribers.put(id, subscriber);
    }

    @Override
    public void stopManaging(String id) {
        this.wasStopped.put(id, true);
    }

    @Override
    public CompletableFuture<?> startPolling() {
        return null;
    }

    @Override
    public CompletableFuture<?> processPollingResults() {
        return null;
    }

    @Override
    public void startGcCycle() {
    }

    public void notifySubscriber(String id, Optional<SpecStatusMeta<Meta, Spec, Status>> data, ObjectLifeCycleEventType eventType) {
        currentSubscribers.get(id).accept(data, eventType);
    }

    public void notifySubscriber(String id, Optional<SpecStatusMeta<Meta, Spec, Status>> data) {
        currentSubscribers.get(id).accept(data, ObjectLifeCycleEventType.UNKNOWN);
    }

    public Spec getCurrentSpec(String id) {
        return currentSpecs.get(id);
    }

    public Acl getCurrentAcl(String id) {
        return currentAcls.get(id);
    }

    @Override
    public CompletableFuture<?> forceCollectGarbage() {
        return null;
    }

    @Override
    public Set<String> getGarbage() {
        return null;
    }

    @Override
    public ClusterAndType getClusterAndType() {
        return null;
    }
}
