package ru.yandex.infra.sidecars_updater.sidecar_service;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.infra.sidecars_updater.StageUpdateNotifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateTaskRunnerTest {
    @ParameterizedTest
    @MethodSource("ru.yandex.infra.sidecars_updater.sidecar_service.SidecarsServiceProxyTest#startNodesSource")
    void successRunTest(Collection<UpdateTaskDTO> startNodes) {
        UpdateTaskRunnerTestBuilder builder = new UpdateTaskRunnerTestBuilder()
                .setStartTable(startNodes)
                .addResultToApply(Pair.of(List.of("123", "132", "312", "321"), List.of()))
                .setWorkerCycleSleep(Duration.ofMillis(1))
                .setAttemptsLimit(3);
        UpdateTaskRunner updateTaskRunner = builder.build();
        int inProgressStates = SidecarsServiceProxyTest.countDTOWithStatus(startNodes, UpdateTask.Status.IN_PROGRESS);
        updateTaskRunner.run();
        while (!updateTaskRunner.getLocalTasks().isEmpty()) {
        }
        if (inProgressStates != 0) {
            Mockito.verify(updateTaskRunner.getYtUpdateTaskController(), atLeastOnce()).insertRows(anyCollection());
        }
        Assertions.assertEquals(builder.failedCounter.get(), SidecarsServiceProxyTest.countDTOWithStatus(startNodes, UpdateTask.Status.FAILED));
        Assertions.assertEquals(builder.successCounter.get(), inProgressStates +
                SidecarsServiceProxyTest.countDTOWithStatus(startNodes, UpdateTask.Status.SUCCESS));
    }

    @ParameterizedTest
    @MethodSource("ru.yandex.infra.sidecars_updater.sidecar_service.SidecarsServiceProxyTest#startNodesSource")
    void failedRunTest(Collection<UpdateTaskDTO> startNodes) {
        UpdateTaskRunnerTestBuilder builder = new UpdateTaskRunnerTestBuilder()
                .setStartTable(startNodes)
                .addResultToApply(Pair.of(List.of(), List.of("123", "132", "312", "321")))
                .setWorkerCycleSleep(Duration.ofMillis(1))
                .setAttemptsLimit(3);
        UpdateTaskRunner updateTaskRunner = builder.build();
        int inProgressStates = SidecarsServiceProxyTest.countDTOWithStatus(startNodes, UpdateTask.Status.IN_PROGRESS);
        updateTaskRunner.run();
        while (!updateTaskRunner.getLocalTasks().isEmpty()) {
        }
        if (inProgressStates != 0) {
            Mockito.verify(updateTaskRunner.getYtUpdateTaskController(), atLeastOnce()).insertRows(anyCollection());
        }
        Assertions.assertEquals(builder.failedCounter.get(), inProgressStates +
                SidecarsServiceProxyTest.countDTOWithStatus(startNodes, UpdateTask.Status.FAILED));
        Assertions.assertEquals(builder.successCounter.get(), SidecarsServiceProxyTest.countDTOWithStatus(startNodes, UpdateTask.Status.SUCCESS));
    }
    static class UpdateTaskRunnerTestBuilder {
        private final AtomicInteger failedCounter = new AtomicInteger(0);
        private final AtomicInteger successCounter = new AtomicInteger(0);
        private final SidecarsService sidecarsService;
        private final StageUpdateNotifier stageUpdateNotifier;
        private final YtUpdateTaskRepository ytUpdateTaskRepository;
        private final UpdateTaskRunner updateTaskRunner;
        private int attemptsLimit;
        private Duration workerCycleSleep;



        private Map<String, UpdateTask> startTable;

        public UpdateTaskRunnerTestBuilder() {
            sidecarsService = mock(SidecarsService.class);
            stageUpdateNotifier = mock(StageUpdateNotifier.class);
            ytUpdateTaskRepository = mock(YtUpdateTaskRepository.class);
            updateTaskRunner = mock(UpdateTaskRunner.class);
            attemptsLimit = 5;
            workerCycleSleep = Duration.ofSeconds(5);
        }
        public UpdateTaskRunnerTestBuilder setAttemptsLimit(int attemptsLimit) {
            this.attemptsLimit = attemptsLimit;
            return this;
        }

        public UpdateTaskRunnerTestBuilder setWorkerCycleSleep(Duration workerCycleSleep) {
            this.workerCycleSleep = workerCycleSleep;
            return this;
        }

        public static void setGettingStatus(YtUpdateTaskRepository ytUpdateTaskRepository,
                                            Map<String, String> statuses) {
            when(ytUpdateTaskRepository.lookupRowStatus(any()))
                    .thenAnswer(it -> Optional.ofNullable(statuses.get(it.<String>getArgument(0))));
        }

        public UpdateTaskRunnerTestBuilder setStartTable(Collection<UpdateTaskDTO> table) {
            startTable = new HashMap<>();
            table.stream()
                    .map(row -> new UpdateTask(row, sidecarsService, stageUpdateNotifier))
                    .forEach(updateTask -> startTable.put(updateTask.getId(), updateTask));
            return this;
        }

        public UpdateTaskRunnerTestBuilder addResultToApply(Pair<List<String>, List<String>> result) {
            when(sidecarsService.applyOnPercent(any(), any(), anyInt(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(result));
            return this;
        }

        public UpdateTaskRunner build() {
            doAnswer(
                    args -> {
                        args.<List<UpdateTask>>getArgument(0)
                                .forEach(row -> {
                                    String status = row.getStatus().name();
                                    if (status.equals(UpdateTask.Status.SUCCESS.name())) {
                                        successCounter.getAndIncrement();
                                    }
                                    if (status.equals(UpdateTask.Status.FAILED.name())) {
                                        failedCounter.getAndIncrement();
                                    }
                                });
                        return null;
                    }
            ).when(ytUpdateTaskRepository).insertRows(anyCollection());
            UpdateTaskRunner updateTaskRunner = new UpdateTaskRunner(sidecarsService, stageUpdateNotifier, ytUpdateTaskRepository, attemptsLimit, workerCycleSleep);
            startTable.forEach((key, value) -> updateTaskRunner.getLocalTasks().put(key, value));
            return updateTaskRunner;
        }
    }
}

