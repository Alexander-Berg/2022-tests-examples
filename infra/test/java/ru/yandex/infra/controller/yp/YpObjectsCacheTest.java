package ru.yandex.infra.controller.yp;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;
import static ru.yandex.infra.controller.util.YpUtils.CommonSelectors.SPEC_STATUS_META;

public class YpObjectsCacheTest {
    private static final long INITIAL_YP_TIMESTAMP = 1234567890L;
    private static final Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> INITIAL_SNAPSHOT =
            Map.of(
                "stage1", createSuccessTry(),
                "stage2", createSuccessTry()
            );

    private MapGaugeRegistry gaugeRegistry;
    private DummyYpObjectTransactionalRepository<StageMeta, TStageSpec, TStageStatus> stageRepository;
    private YpObjectsCache<StageMeta, TStageSpec, TStageStatus> cache;

    @BeforeEach
    void before() {
        gaugeRegistry = new MapGaugeRegistry();
        stageRepository = new DummyYpObjectTransactionalRepository<>();
        YpObjectSettings ypObjectSettings = new YpObjectSettings.Builder()
                .setWatches(true)
                .setGetObjectsBatchSize(10)
                .build();
        cache = new YpObjectsCache<>(stageRepository, ypObjectSettings, gaugeRegistry, SPEC_STATUS_META);
        stageRepository.selectResponse.complete(new SelectedObjects<>(INITIAL_SNAPSHOT, INITIAL_YP_TIMESTAMP));
    }

    private static Try<YpObject<StageMeta, TStageSpec, TStageStatus>> createSuccessTry() {
        return Try.success(createObject());
    }

    private static YpObject<StageMeta, TStageSpec, TStageStatus> createObject() {
        return createObject(0);
    }

    private static YpObject<StageMeta, TStageSpec, TStageStatus> createObject(int revision) {
        return new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpec(TStageSpec.newBuilder()
                        .setRevision(revision)
                        .build())
                .build();
    }

    private int getRevision(YpObject<StageMeta, TStageSpec, TStageStatus> object) {
        return object.getSpec().getRevision();
    }

    @Test
    void simpleSelectTest() {
        long timestamp = 777;
        var result = get1s(cache.selectObjects(Optional.of(timestamp)));
        assertThat(result, equalTo(INITIAL_SNAPSHOT));
        assertThat(stageRepository.lastUsedSelectObjectsTimestamp, equalTo(timestamp));
    }

    @Test
    void secondCallShouldUseWatchesInsteadOfSelectTest() {
        get1s(cache.selectObjects(Optional.empty()));

        assertThat(stageRepository.selectCallsCount, equalTo(1));
        assertThat(stageRepository.watchCallsCount, equalTo(0));

        long watchResultTimestamp = 222;
        stageRepository.watchResponse.complete(new WatchedObjects(Map.of(), watchResultTimestamp));
        var result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(stageRepository.selectCallsCount, equalTo(1));
        assertThat(stageRepository.watchCallsCount, equalTo(1));
        assertThat(result, equalTo(INITIAL_SNAPSHOT));
        assertThat(stageRepository.lastUsedWatchObjectsTimestamp, equalTo(INITIAL_YP_TIMESTAMP));

        result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(stageRepository.selectCallsCount, equalTo(1));
        assertThat(stageRepository.watchCallsCount, equalTo(2));
        assertThat(stageRepository.getObjectsCallsCount, equalTo(0));
        assertThat(result, equalTo(INITIAL_SNAPSHOT));
        assertThat(stageRepository.lastUsedWatchObjectsTimestamp, equalTo(watchResultTimestamp));
        assertThat(gaugeRegistry.getGaugeValue(YpObjectsCache.METRIC_FULL_RELOAD_COUNT), equalTo(1L));
    }

    @Test
    void getObjectsNewItemTest() {
        get1s(cache.selectObjects(Optional.empty()));

        final long watchResultsTimestamp = 111L;
        stageRepository.watchResponse.complete(new WatchedObjects(Map.of("stage3", List.of()), watchResultsTimestamp));
        stageRepository.getObjectsTransactionalResponse.put(watchResultsTimestamp, CompletableFuture.completedFuture(List.of(Optional.of(createObject()))));
        var result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(result.size(), equalTo(INITIAL_SNAPSHOT.size() + 1));
    }

    @Test
    void getObjectsUpdatedItemTest() {

        final String stageFromInitialSnapshot = INITIAL_SNAPSHOT.keySet().stream().findFirst().orElseThrow();
        var result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(getRevision(result.get(stageFromInitialSnapshot).get()), equalTo(0));

        final long watchResultsTimestamp = 111L;
        stageRepository.watchResponse.complete(new WatchedObjects(Map.of(stageFromInitialSnapshot, List.of()), watchResultsTimestamp));
        int updatedRevision = 2;
        stageRepository.getObjectsTransactionalResponse.put(watchResultsTimestamp,
                CompletableFuture.completedFuture(List.of(Optional.of(createObject(updatedRevision)))));

        result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(result.size(), equalTo(INITIAL_SNAPSHOT.size()));
        assertThat(getRevision(result.get(stageFromInitialSnapshot).get()), equalTo(updatedRevision));
    }

    @Test
    void getObjectsRemovedItemTest() {

        final String stageFromInitialSnapshot = INITIAL_SNAPSHOT.keySet().stream().findFirst().orElseThrow();
        var result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(result.size(), equalTo(INITIAL_SNAPSHOT.size()));

        final long watchResultsTimestamp = 111L;
        stageRepository.watchResponse.complete(new WatchedObjects(Map.of(stageFromInitialSnapshot, List.of()), watchResultsTimestamp));
        stageRepository.getObjectsTransactionalResponse.put(watchResultsTimestamp,
                CompletableFuture.completedFuture(List.of(Optional.empty())));

        result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(result.size(), equalTo(INITIAL_SNAPSHOT.size() - 1));
        assertThat(result.get(stageFromInitialSnapshot), nullValue());
    }

    @Test
    void noChangesTest() {
        get1s(cache.selectObjects(Optional.empty()));

        assertThat(stageRepository.selectCallsCount, equalTo(1));
        assertThat(stageRepository.watchCallsCount, equalTo(0));
        assertThat(stageRepository.getObjectsCallsCount, equalTo(0));

        final long watchResultsTimestamp = 111L;
        stageRepository.watchResponse.complete(new WatchedObjects(Map.of(), watchResultsTimestamp));
        var result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(result, equalTo(INITIAL_SNAPSHOT));
        assertThat(stageRepository.selectCallsCount, equalTo(1));
        assertThat(stageRepository.watchCallsCount, equalTo(1));
        assertThat(stageRepository.getObjectsCallsCount, equalTo(0));
    }


    @Test
    void fallbackToSelectObjectsAfterExceptionTest() {
        get1s(cache.selectObjects(Optional.empty()));

        assertThat(stageRepository.selectCallsCount, equalTo(1));
        assertThat(stageRepository.watchCallsCount, equalTo(0));
        assertThat(stageRepository.getObjectsCallsCount, equalTo(0));

        stageRepository.watchResponse.completeExceptionally(new RuntimeException("watches failed"));
        var result = get1s(cache.selectObjects(Optional.empty()));

        assertThat(result, equalTo(INITIAL_SNAPSHOT));
        assertThat(stageRepository.selectCallsCount, equalTo(2));
        assertThat(stageRepository.watchCallsCount, equalTo(1));
        assertThat(stageRepository.getObjectsCallsCount, equalTo(0));
        assertThat(gaugeRegistry.getGaugeValue(YpObjectsCache.METRIC_FULL_RELOAD_COUNT), equalTo(2L));
        assertThat(gaugeRegistry.getGaugeValue(YpObjectsCache.METRIC_WATCH_OBJECTS_ERRORS_COUNT), equalTo(1L));
        assertThat(gaugeRegistry.getGaugeValue(YpObjectsCache.METRIC_YP_OBJECTS_LOAD_ERRORS_COUNT), equalTo(0L));
    }

}
