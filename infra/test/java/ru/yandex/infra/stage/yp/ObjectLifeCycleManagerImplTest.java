package ru.yandex.infra.stage.yp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.yp.CreateObjectRequest;
import ru.yandex.infra.controller.yp.DummyYpObjectTransactionalRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.WatchedObjects;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.controller.yp.YpObjectSettings;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.util.SettableClock;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

class ObjectLifeCycleManagerImplTest {
    private static final String OBJECT_ID = "id";
    private static final TReplicaSetSpec SPEC = TReplicaSetSpec.getDefaultInstance();
    private static final CreateObjectRequest<TReplicaSetSpec> SPEC_WITH_ACL_CREATE_REQUEST =
            new CreateObjectRequest.Builder<>(SPEC).setAcl(TestData.STAGE_ACL).build();
    private static final TReplicaSetStatus STATUS = TReplicaSetStatus.getDefaultInstance();
    private static final YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> SPEC_AND_STATUS =
            new YpObject.Builder<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>()
                    .setSpecAndTimestamp(SPEC, 1)
                    .setStatus(STATUS)
                    .setMeta(new SchemaMeta(OBJECT_ID, TestData.STAGE_ACL, "", "", 0))
                    .build();
    private static final Try<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>> TRY_SPEC_AND_STATUS =
            Try.success(SPEC_AND_STATUS);
    private static final String NOT_GARBAGE_APPROVED_ID = "NOT_APPROVED_ID";

    private SerialExecutor serialExecutor;
    private DummyYpObjectTransactionalRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> repository;
    private ObjectLifeCycleManagerImpl<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> manager;
    private MapGaugeRegistry gaugeRegistry;
    private SettableClock clock;
    private YpObjectSettings ypObjectSettings;

    @BeforeEach
    void before() {
        serialExecutor = new SerialExecutor(getClass().getSimpleName());
        repository = new DummyYpObjectTransactionalRepository<>();
        gaugeRegistry = new MapGaugeRegistry();
        clock = new SettableClock();
        ypObjectSettings = new YpObjectSettings.Builder()
                .setWatches(true)
                .setGetObjectsBatchSize(10)
                .build();
        manager = new ObjectLifeCycleManagerImpl<>(repository, serialExecutor, gaugeRegistry, clock,
                (id, clusterAndType) -> {
                    boolean isRetained = id.equals(NOT_GARBAGE_APPROVED_ID) && clusterAndType.equals(TestData.CLUSTER_AND_TYPE);
                    return new Retainment(isRetained, "some test");
                }, TestData.CLUSTER_AND_TYPE, 2, 2,
                TestData.CLUSTER_AND_TYPE.toString(),
                ypObjectSettings);
    }

    @AfterEach
    void after() {
        serialExecutor.shutdown();
    }

    private void poll() {
        get1s(manager.startPolling());
        get1s(manager.processPollingResults());
    }

    @Test
    void passEmptyOptionalForAbsentObject() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(), 0L));
        CompletableFuture<Optional<SpecStatusMeta<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> future = new CompletableFuture<>();
        manager.startManaging(OBJECT_ID, (specStatusMeta, eventType) -> future.complete(specStatusMeta));
        poll();
        assertThat(get1s(future), equalTo(Optional.empty()));
    }

    @Test
    void removeUnknownObjectOnGarbageCollection() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        poll();
        manager.startGcCycle();
        assertThat(repository.removedIds, contains(OBJECT_ID));
    }

    @Test
    void notRemoveUnapprovedUnknownObjectOnGarbageCollection() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(NOT_GARBAGE_APPROVED_ID, TRY_SPEC_AND_STATUS), 0L));
        poll();
        manager.startGcCycle();
        assertThat(repository.removedIds, not(contains(NOT_GARBAGE_APPROVED_ID)));
    }

    @Test
    void notToggleSubscriberForCurrentlyUpdated() {
        notToggleSubscriberDuringActionTestTemplate(
                () -> manager.updateSpecWithLabels(OBJECT_ID, SPEC, emptyMap(), Optional.empty(), () -> {}, ignored -> {}),
                repository.updateResponse
        );
    }

    @Test
    void notToggleSubscriberForCurrentlyCreated() {
        notToggleSubscriberDuringActionTestTemplate(
                () -> manager.create(OBJECT_ID, SPEC_WITH_ACL_CREATE_REQUEST, () -> {}, ignored -> {}),
                repository.createResponse
        );
    }

    @Test
    void moveToGarbageAfterStoppingManaging() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> {});
        manager.stopManaging(OBJECT_ID);
        poll();
        manager.startGcCycle();
        assertThat(repository.removedIds, contains(OBJECT_ID));
    }

    @Test
    void startManagingWhileOperationIsRunning() {
        repository.createResponse = new CompletableFuture<>();
        manager.create(OBJECT_ID, SPEC_WITH_ACL_CREATE_REQUEST, () -> {}, ignored -> {});
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> {});
    }

    @Test
    void reportManagedObjectsMetric() {
        repository.getResponses.put(OBJECT_ID, new CompletableFuture<>());
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> {});
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_MANAGED_OBJECTS), equalTo(1));
    }

    @Test
    void reportDelayMetric() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        clock.incrementSecond();
        poll();
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_LIST_DELAY_SECONDS), equalTo(0L));
        clock.incrementSecond();
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_LIST_DELAY_SECONDS), equalTo(1L));
    }

    @Test
    void reportRunningOperationsMetric() {
        repository.getResponses.put(OBJECT_ID, new CompletableFuture<>());
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> {});
        manager.create(OBJECT_ID, SPEC_WITH_ACL_CREATE_REQUEST, () -> {}, ignored -> {});
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_RUNNING_OPERATIONS), equalTo(1));
    }

    @Test
    void reportGarbageCountMetric() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        poll();
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_GARBAGE_OBJECTS), equalTo(1));
    }

    @Test
    void allowReplacingExistingSubscription() {
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> {});
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> {});
    }

    @Test
    void reportFailedOperationsMetric() {
        failToCreateObject();
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_OBJECTS_WITH_FAILED_OPERATIONS_COUNT), equalTo(1));

        repository.createResponse = completedFuture(null);
        CompletableFuture<?> created = new CompletableFuture<>();
        manager.create(OBJECT_ID, SPEC_WITH_ACL_CREATE_REQUEST, () -> created.complete(null), ignored -> {});
        get1s(created);
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_OBJECTS_WITH_FAILED_OPERATIONS_COUNT), equalTo(0));
    }

    @Test
    void removeRemovedObjectsFromFailedOperations() {
        failToCreateObject();
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        poll();
        manager.startGcCycle();
        assertThat(gaugeRegistry.getGaugeValue(ObjectLifeCycleManagerImpl.METRIC_OBJECTS_WITH_FAILED_OPERATIONS_COUNT), equalTo(0));
    }

    @Test
    void notForceGarbageCollectTest() {
        setupForGarbageTest(1, 1);
        pollWithGarbage();

        manager.startGcCycle();
        assertThat(repository.removedIds, empty());

        get1s(manager.forceCollectGarbage());
        manager.startGcCycle();
        assertThat(repository.removedIds, hasSize(2));
    }

    @Test
    void separateLimitForFirstGarbageCollection() {
        setupForGarbageTest(100, 1);
        pollWithGarbage();

        manager.startGcCycle();
        assertThat(repository.removedIds, empty());

        // flag is not lost after gc has been denied
        manager.startGcCycle();
        assertThat(repository.removedIds, empty());

        get1s(manager.forceCollectGarbage());
        manager.startGcCycle();
        assertThat(repository.removedIds, hasSize(2));
        repository.removeResponse.complete(null);

        repository.removedIds.clear();

        // next iterations apply common limits
        pollWithGarbage();

        manager.startGcCycle();
        assertThat(repository.removedIds, hasSize(2));
    }

    @Test
    void eventType() {
        repository.getResponses.put(OBJECT_ID, completedFuture(Optional.of(SPEC_AND_STATUS)));
        repository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        List<Tuple2<Optional<SpecStatusMeta<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>, ObjectLifeCycleEventType>> events = new ArrayList<>();
        manager.startManaging(OBJECT_ID, (obj, eventType) -> events.add(Tuple2.tuple(obj, eventType)));
        poll();
        assertThat(events, hasSize(1));
        assertThat(events.get(0)._2, equalTo(ObjectLifeCycleEventType.RELOADED));
    }

    @Test
    void removeEvent() {
        repository.getResponses.put(OBJECT_ID, completedFuture(Optional.of(SPEC_AND_STATUS)));

        List<CompletableFuture<?>> callBackFutures = List.of(new CompletableFuture<>(), new CompletableFuture<>());
        List<Tuple2<Optional<SpecStatusMeta<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>, ObjectLifeCycleEventType>> events = new ArrayList<>();
        manager.startManaging(OBJECT_ID, (obj, eventType) -> {
            events.add(Tuple2.tuple(obj, eventType));
            callBackFutures.get(events.size()-1).complete(null);
        });
        repository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(), 0L));
        poll();
        assertThat(events, hasSize(1));
        assertThat(events.get(0)._2, equalTo(ObjectLifeCycleEventType.MISSED));

        manager.stopManaging(OBJECT_ID);

        repository.removeResponse.complete(null);
        manager.startGcCycle();
        get1s(callBackFutures.get(1));

        assertThat(events, hasSize(2));
        assertThat(events.get(1)._2, equalTo(ObjectLifeCycleEventType.REMOVED));
    }

    @Test
    void dontProcessResultsAfterPollingStart() {
        repository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));

        AtomicInteger callbackCallsCount = new AtomicInteger();
        manager.startManaging(OBJECT_ID, (ignored, eventType) -> callbackCallsCount.incrementAndGet());

        get1s(manager.startPolling());
        assertThat(callbackCallsCount.get(), equalTo(0));

        get1s(manager.processPollingResults());
        assertThat(callbackCallsCount.get(), equalTo(1));
    }

    @Test
    void toStringTest() {
        assertThat(manager.toString(), equalTo(TestData.CLUSTER_AND_TYPE.toString()));
    }

    private void pollWithGarbage() {
        manager.startPolling();
        repository.selectResponse.complete(new SelectedObjects<>(ImmutableMap.of("id1", TRY_SPEC_AND_STATUS, "id2", TRY_SPEC_AND_STATUS,
                "id3", TRY_SPEC_AND_STATUS, "id4", TRY_SPEC_AND_STATUS), 0L));
        repository.watchResponse.complete(new WatchedObjects(Map.of(), 0L));
        get1s(manager.processPollingResults());
    }

    private void setupForGarbageTest(int garbageCountLimit, int initialGarbageCountLimit) {
        manager = new ObjectLifeCycleManagerImpl<>(repository, serialExecutor, gaugeRegistry, clock,
                (a, b) -> new Retainment(false, ""), TestData.CLUSTER_AND_TYPE, garbageCountLimit,
                initialGarbageCountLimit, TestData.CLUSTER_AND_TYPE.toString(), ypObjectSettings);
        repository.selectResponse = new CompletableFuture<>();
        manager.startManaging("id1", (ignored, eventType) -> {});
        manager.startManaging("id2", (ignored, eventType) -> {});
    }

    private void failToCreateObject() {
        repository.createResponse = CompletableFuture.failedFuture(new RuntimeException());
        CompletableFuture<?> createFailed = new CompletableFuture<>();
        manager.create(OBJECT_ID, SPEC_WITH_ACL_CREATE_REQUEST, () -> {}, ignored -> createFailed.complete(null));
        get1s(createFailed);
    }

    private void notToggleSubscriberDuringActionTestTemplate(Runnable actionStart, CompletableFuture<?> actionFuture) {
        repository.selectResponse = completedFuture(new SelectedObjects<>(
                ImmutableMap.of(OBJECT_ID, TRY_SPEC_AND_STATUS), 0L));
        repository.watchResponse.complete(new WatchedObjects(Map.of(), 0L));
        CompletableFuture<Optional<SpecStatusMeta<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> future = new CompletableFuture<>();
        manager.startManaging(OBJECT_ID, (specStatusMeta, eventType) -> future.complete(specStatusMeta));
        actionStart.run();
        poll();
        assertNotDone(future);
        actionFuture.complete(null);
        poll();
        get1s(future);
    }

    private static void assertNotDone(CompletableFuture<?> future) {
        assertThat("Future should not be completed", !future.isDone());
    }
}
