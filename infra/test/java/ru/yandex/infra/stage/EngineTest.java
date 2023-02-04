package ru.yandex.infra.stage;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import ru.yandex.infra.controller.RepeatedTask;
import ru.yandex.infra.controller.concurrent.DummyLeaderService;
import ru.yandex.infra.controller.concurrent.LeaderService;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.metrics.NamespacedMapGaugeRegistry;
import ru.yandex.infra.controller.yp.DummyYpObjectTransactionalRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.WatchedObjects;
import ru.yandex.infra.controller.yp.YpObjectSettings;
import ru.yandex.infra.controller.yp.YpObjectTransactionalRepository;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.model.YpException;
import ru.yandex.yp.model.YpObjectType;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;
import static ru.yandex.infra.controller.util.YpUtils.CommonSelectors.SPEC_META;

class EngineTest {
    private static final Duration EXTERNAL_RESOURCES_TIMEOUT = Duration.ofMillis(100);

    private SerialExecutor serialExecutor;
    private MapGaugeRegistry gaugeRegistry;
    private NamespacedMapGaugeRegistry engineMetrics;
    private DummyRootController rootController;
    private DummyYpObjectTransactionalRepository<StageMeta, TStageSpec, TStageStatus> stageRepository;
    private DummyYpObjectTransactionalRepository<SchemaMeta, TProjectSpec, TProjectStatus> projectRepository;
    private Engine engine;
    private Map<YpObjectType, YpObjectSettings> ypCacheSettings;

    @BeforeEach
    void before() {
        serialExecutor = new SerialExecutor("serial");
        gaugeRegistry = new MapGaugeRegistry();
        engineMetrics = new NamespacedMapGaugeRegistry("engine", gaugeRegistry);
        rootController = new DummyRootController();
        stageRepository = new DummyYpObjectTransactionalRepository<>();
        projectRepository = new DummyYpObjectTransactionalRepository<>();
        ypCacheSettings = Map.of(YpObjectSettings.TYPE_USED_AS_KEY_WITH_DEFAULT_SETTINGS, new YpObjectSettings.Builder().setWatches(false).build());
        engine = new Engine(stageRepository,
                projectRepository,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                EXTERNAL_RESOURCES_TIMEOUT,
                Duration.ofSeconds(1),
                rootController,
                serialExecutor,
                new DummyLeaderService(new MetricRegistry()),
                Collections.emptySet(),
                gaugeRegistry,
                ypCacheSettings);
    }

    @AfterEach
    void after() {
        serialExecutor.shutdown();
    }

    @Test
    void recoverFromExceptions() {
        CompletableFuture<?> wasSynced = new CompletableFuture<>();

        AtomicInteger hangCounterRef = new AtomicInteger(0);
        AtomicInteger leaderCounterRef = new AtomicInteger(0);
        AtomicInteger rootControllerCounerRef = new AtomicInteger(0);
        AtomicInteger repositoryCounterRef = new AtomicInteger(0);

        long requiredHangErrors = 1;
        long requiredLeaderLockErrors = 1;
        long requiredYpErrors = 3;
        long requiredRootControllerSyncErrors = 1;
        long requiredSuccessRootControllerSyncBeforeFinalStop = 2;

        rootController.updateStatusesAction = () -> {
            succeedAfterFirstTry(rootControllerCounerRef, requiredRootControllerSyncErrors);

            //allow requiredSuccessRootControllerSyncBeforeFinalStop success executions and then stop
            if (rootControllerCounerRef.get() == requiredRootControllerSyncErrors+requiredSuccessRootControllerSyncBeforeFinalStop) {
                serialExecutor.schedule(() -> {
                    //fire event, that we are ready to check all collected metrics
                    wasSynced.complete(null);
                    //Stopping Engine iterations
                    serialExecutor.shutdown();
                }, Duration.ZERO);
            }
        };

        LeaderService throwingService = Mockito.mock(LeaderService.class);
        Mockito.doAnswer(invocation -> {
            succeedAfterFirstTry(leaderCounterRef, requiredLeaderLockErrors);
            return true;
        }).when(throwingService).isLeader();

        YpObjectTransactionalRepository<StageMeta, TStageSpec, TStageStatus> stageRepository =
                Mockito.mock(YpObjectTransactionalRepository.class);
        YpObjectTransactionalRepository<SchemaMeta, TProjectSpec, TProjectStatus> projectRepository =
                Mockito.mock(YpObjectTransactionalRepository.class);

        Duration engineTimeout = Duration.ofMillis(300);

        Mockito.doReturn(YpObjectType.STAGE).when(stageRepository).getObjectType();
        Mockito.doReturn(YpObjectType.PROJECT).when(projectRepository).getObjectType();

        Mockito.doReturn(completedFuture(1L)).when(stageRepository).generateTimestamp();
        Mockito.doAnswer(invocation -> {
            succeedAfterFirstTry(repositoryCounterRef, requiredYpErrors);

            CompletableFuture<SelectedObjects<StageMeta, TStageSpec, TStageStatus>> completableFuture = new CompletableFuture<>();
            Executors.newCachedThreadPool().submit(() -> {
                //emulation of hung task
                if(hangCounterRef.incrementAndGet() <= requiredHangErrors) {
                    Thread.sleep(engineTimeout.toMillis() + 100500);
                }
                completableFuture.complete(new SelectedObjects<>(emptyMap(), 1L));
                return null;
            });

            return completableFuture;
        }).when(projectRepository).selectObjects(SPEC_META, emptyMap(), 1L);

        Mockito.doReturn(completedFuture(new WatchedObjects(Map.of(), 1L))).when(projectRepository)
                .watchObjects(1L, Optional.empty());

        Mockito.doReturn(completedFuture(Collections.emptySet())).when(stageRepository).listAllIds(1L);
        Mockito.doReturn(completedFuture(new SelectedObjects<>(emptyMap(), 1L))).when(stageRepository)
                .selectObjects(any(), any(), any());
        Mockito.doReturn(completedFuture(new WatchedObjects(Map.of(), 1L))).when(stageRepository)
                        .watchObjects(1L, Optional.empty());

        Mockito.doAnswer(invocation -> completedFuture(Collections.emptyList()))
                .when(stageRepository).getObjects(any(), any());

        Duration syncInterval = Duration.ofMillis(1);

        Engine engine = new Engine(stageRepository,
                projectRepository,
                syncInterval,
                engineTimeout,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                rootController,
                serialExecutor,
                throwingService,
                Collections.emptySet(),
                gaugeRegistry,
                ypCacheSettings);
        engine.start();

        get5s(wasSynced);
        assertThat(leaderCounterRef.get(), greaterThan(0));
        assertThat(rootControllerCounerRef.get(), greaterThan(0));
        assertThat(repositoryCounterRef.get(), greaterThan(0));

        long expectedFailedIterations = requiredHangErrors +
                requiredLeaderLockErrors +
                requiredYpErrors +
                requiredRootControllerSyncErrors;
        assertThat(engineMetrics.getGaugeValue(RepeatedTask.METRIC_FAILED_ITERATIONS_COUNT), equalTo(expectedFailedIterations));
        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_YP_OBJECTS_LOAD_ERRORS_COUNT), equalTo(requiredYpErrors));
        assertThat(engineMetrics.getGaugeValue(RepeatedTask.METRIC_HUNG_ITERATIONS_COUNT), equalTo(requiredHangErrors));

        long expectedTotalIterations = expectedFailedIterations + requiredSuccessRootControllerSyncBeforeFinalStop;
        assertThat(engineMetrics.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT), equalTo(expectedTotalIterations));
    }

    private void succeedAfterFirstTry(AtomicInteger counterRef, long requiredFailuresCount) {
        var attemptNumber = counterRef.incrementAndGet();
        if (attemptNumber <= requiredFailuresCount) {
            throw new RuntimeException(attemptNumber + "th attempt fails");
        }
    }

    @Test
    void noObjectsTest() {
        stageRepository.generateTimestampResponse.complete(0L);
        stageRepository.listAllIdsResponse.complete(Collections.emptySet());
        stageRepository.selectResponse.complete(new SelectedObjects<>(emptyMap(), 0L));
        projectRepository.selectResponse.complete(new SelectedObjects<>(emptyMap(), 0L));

        get5s(engine.mainLoop());

        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_YP_OBJECTS_LOAD_ERRORS_COUNT), equalTo(0L));
        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_CHILD_OBJECTS_GC_ERRORS_COUNT), equalTo(0L));
    }

    @Test
    void fetchStageIdsAndProjectsWithTheSameTimestampTest() {
        long expectedTimestamp = 555L;
        stageRepository.generateTimestampResponse.complete(expectedTimestamp);
        stageRepository.listAllIdsResponse.complete(Collections.emptySet());
        stageRepository.selectResponse.complete(new SelectedObjects<>(emptyMap(), 0L));
        projectRepository.selectResponse.complete(new SelectedObjects<>(emptyMap(), 0L));

        get5s(engine.mainLoop());

        assertThat(stageRepository.lastUsedListAllIdsTimestamp, equalTo(Optional.of(expectedTimestamp)));
        assertThat(stageRepository.lastUsedSelectObjectsTimestamp, equalTo(expectedTimestamp));
        assertThat(projectRepository.lastUsedSelectObjectsTimestamp, equalTo(expectedTimestamp));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "false,false,false,false",
            "true,false,false,false",
            "false,true,false,false",
            "false,false,true,false",
            "false,false,false,true",
            "true,true,true,true",
    })
    void shouldNotDeleteObjectsIfLoadingOfYpObjectsFailedTest(boolean failTimestamp,
                                                              boolean failListAllIds,
                                                              boolean failStagesSelect,
                                                              boolean failProjectSelect) {
        var exception = new YpException(null, null, null, null);
        if (failTimestamp) {
            stageRepository.generateTimestampResponse.completeExceptionally(exception);
        } else {
            stageRepository.generateTimestampResponse.complete(0L);
        }

        if (failListAllIds) {
            stageRepository.listAllIdsResponse.completeExceptionally(exception);
        } else {
            stageRepository.listAllIdsResponse.complete(Collections.emptySet());
        }

        if (failStagesSelect) {
            stageRepository.selectResponse.completeExceptionally(exception);
        } else {
            stageRepository.selectResponse.complete(new SelectedObjects<>(emptyMap(), 0L));
        }

        if (failProjectSelect) {
            projectRepository.selectResponse.completeExceptionally(exception);
        } else {
            projectRepository.selectResponse.complete(new SelectedObjects<>(emptyMap(), 0L));
        }

        boolean failureIsExpected = failTimestamp || failListAllIds || failStagesSelect || failProjectSelect;

        if (failureIsExpected) {
            assertThrows(YpException.class, () -> get5s(engine.mainLoop()));
        } else {
            get5s(engine.mainLoop());
        }

        assertThat(rootController.processGcForRemovedStagesCalls, equalTo(failureIsExpected ? 0 : 1));
        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_YP_OBJECTS_LOAD_ERRORS_COUNT), equalTo(failureIsExpected ? 1L : 0L));
    }

    @Test
    void singleStageTest() {
        Set<String> allStageIds = Set.of(TestData.DEFAULT_STAGE_ID, "stage_with_unknown_label");
        Set<String> stageIds = Set.of(TestData.DEFAULT_STAGE_ID);

        stageRepository.generateTimestampResponse.complete(0L);
        stageRepository.listAllIdsResponse.complete(allStageIds);
        stageRepository.selectResponse.complete(new SelectedObjects<>(Map.of(TestData.DEFAULT_STAGE_ID, TestData.STAGE), 0L));
        projectRepository.selectResponse.complete(new SelectedObjects<>(Map.of(TestData.PROJECT_ID, TestData.PROJECT), 0L));

        get1s(engine.mainLoop());

        assertThat(rootController.processGcForRemovedStagesCalls, equalTo(1));
        assertThat(rootController.lastAllStageIds, equalTo(allStageIds));
        assertThat(rootController.lastStageIdsWithDeployEngine, equalTo(stageIds));

        assertThat(rootController.syncCalls, equalTo(1));
        assertThat(rootController.lastSyncStages.get(0).keySet(), equalTo(stageIds));
        assertThat(rootController.lastSyncProjects.keySet(), equalTo(Set.of(TestData.PROJECT_ID)));
        assertThat(rootController.updateStatusesCalls, equalTo(1));

        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_YP_OBJECTS_LOAD_ERRORS_COUNT), equalTo(0L));
        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_CHILD_OBJECTS_GC_ERRORS_COUNT), equalTo(0L));
    }

    @Test
    void serialExecutorSubmittedFutureTimeoutTest() {
        Set<String> stageIds = Set.of(TestData.DEFAULT_STAGE_ID);

        stageRepository.generateTimestampResponse.complete(0L);
        stageRepository.listAllIdsResponse.complete(stageIds);
        stageRepository.selectResponse.complete(new SelectedObjects<>(Map.of(TestData.DEFAULT_STAGE_ID, TestData.STAGE), 0L));
        projectRepository.selectResponse.complete(new SelectedObjects<>(Map.of(TestData.PROJECT_ID, TestData.PROJECT), 0L));

        var dockerResolverRequestFuture = new CompletableFuture<>();
        rootController.syncAction = () -> serialExecutor.submitFuture(dockerResolverRequestFuture, null, null);
        var mainLoopFuture = engine.mainLoop();

        serialExecutor.schedule(() -> dockerResolverRequestFuture.complete("resolved"), EXTERNAL_RESOURCES_TIMEOUT.multipliedBy(2));
        get5s(mainLoopFuture);

        //Future should not be cancelled
        assertThat(get1s(dockerResolverRequestFuture), equalTo("resolved"));

        assertThat(rootController.updateStatusesCalls, equalTo(1));
        assertThat(engineMetrics.getGaugeValue(Engine.METRIC_SERIAL_EXECUTOR_TASKS_WAIT_TIMEOUTS), equalTo(1L));
    }
}
