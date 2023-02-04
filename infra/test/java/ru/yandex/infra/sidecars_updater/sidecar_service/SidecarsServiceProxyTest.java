package ru.yandex.infra.sidecars_updater.sidecar_service;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.infra.sidecars_updater.StageUpdateNotifier;
import ru.yandex.infra.sidecars_updater.sidecars.PodAgentSidecar;
import ru.yandex.infra.sidecars_updater.sidecars.Sidecar;
import ru.yandex.infra.sidecars_updater.startrek.DummySession;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SidecarsServiceProxyTest {
    public static final Sidecar DEFAULT_SIDECAR = new PodAgentSidecar(Map.of(Sidecar.Type.POD_AGENT_BINARY, Map.of()));

    public static final int OLD_PATCHERS_REVISION = 3;
    public static final int ANOTHER_OLD_PATCHERS_REVISION = OLD_PATCHERS_REVISION + 1;

    private static UpdateTaskDTO createUpdateTaskDTO(String id, UpdateTask.Status status) {
        return new UpdateTaskDTO(id, DEFAULT_SIDECAR.getResourceType().name(), 0, 10, "test", 0, status.name());
    }

    static Stream<Arguments> startNodesSource() {
        return Stream.of(
                Arguments.of(Collections.emptyList()),
                Arguments.of(List.of(
                        createUpdateTaskDTO("1", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("2", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("3", UpdateTask.Status.FAILED),
                        createUpdateTaskDTO("4", UpdateTask.Status.SUCCESS))),
                Arguments.of(List.of(
                        createUpdateTaskDTO("1", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("2", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("3", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("4", UpdateTask.Status.FAILED),
                        createUpdateTaskDTO("5", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("6", UpdateTask.Status.SUCCESS))),
                Arguments.of(List.of(
                        createUpdateTaskDTO("1", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("2", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("3", UpdateTask.Status.FAILED),
                        createUpdateTaskDTO("4", UpdateTask.Status.FAILED),
                        createUpdateTaskDTO("5", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("6", UpdateTask.Status.SUCCESS),
                        createUpdateTaskDTO("7", UpdateTask.Status.SUCCESS))),
                Arguments.of(List.of(
                        createUpdateTaskDTO("1", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("2", UpdateTask.Status.IN_PROGRESS),
                        createUpdateTaskDTO("3", UpdateTask.Status.IN_PROGRESS))),
                Arguments.of(List.of(
                        createUpdateTaskDTO("1", UpdateTask.Status.FAILED),
                        createUpdateTaskDTO("2", UpdateTask.Status.FAILED),
                        createUpdateTaskDTO("3", UpdateTask.Status.FAILED)
                )),
                Arguments.of(List.of(
                        createUpdateTaskDTO("1", UpdateTask.Status.SUCCESS),
                        createUpdateTaskDTO("2", UpdateTask.Status.SUCCESS),
                        createUpdateTaskDTO("3", UpdateTask.Status.SUCCESS)
                ))
        );
    }

    static Issue createIssue(String id, String key) {
        return new Issue(id, URI.create(""), key, "", 1, Cf.map(), new DummySession());
    }

    static Stream<Arguments> applyOnPercentSource() {
        return Stream.of(
                Arguments.of(Optional.of(DEFAULT_SIDECAR), OptionalInt.empty(), createIssue("a", "1")),
                Arguments.of(Optional.of(DEFAULT_SIDECAR), OptionalInt.of(OLD_PATCHERS_REVISION), createIssue("b", "2"
                )),
                Arguments.of(Optional.empty(), OptionalInt.of(ANOTHER_OLD_PATCHERS_REVISION), createIssue("c", "3"))
        );
    }

    static Stream<Arguments> getStatusSource() {
        return startNodesSource()
                .map(testCase -> Arguments.of(testCase.get()[0],
                        ((Collection<UpdateTaskDTO>) (testCase.get()[0])).stream()
                                .map(row -> Pair.of(row.getId(), row.getStatus()))
                                .collect(Collectors.toMap(Pair::getKey, Pair::getValue))));
    }

    static Stream<UpdateTaskDTO> getDTOStreamWithStatus(Collection<UpdateTaskDTO> nodes, UpdateTask.Status status) {
        return nodes.stream().filter(row -> row.getStatus().equals(status.name()));
    }

    static int countDTOWithStatus(Collection<UpdateTaskDTO> nodes, UpdateTask.Status status) {
        return (int) getDTOStreamWithStatus(nodes, status).count();
    }

    @ParameterizedTest
    @MethodSource("startNodesSource")
    void checkLocalMapFillingTest(List<UpdateTaskDTO> nodes) {
        SidecarsServiceProxyTestBuilder builder = new SidecarsServiceProxyTestBuilder().setStartTable(nodes);
        SidecarsServiceProxy sidecarsServiceProxy = builder.build();
        Assertions.assertEquals(builder.tasks.keySet(),
                getDTOStreamWithStatus(nodes, UpdateTask.Status.IN_PROGRESS)
                        .map(row -> row.getId())
                        .collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @MethodSource("applyOnPercentSource")
    void applyOnPercentTest(Optional<Sidecar> sidecar, OptionalInt patchersRevision, Issue issue) {
        SidecarsServiceProxyTestBuilder builder = new SidecarsServiceProxyTestBuilder().setCreatingIssue(issue);
        SidecarsServiceProxy sidecarsServiceProxy = builder.build();
        Pair<String, String> idAndRef = sidecarsServiceProxy.applyOnPercent(sidecar, patchersRevision, 10, "test");
        if (issue != null) {
            Assertions.assertEquals(issue.getId(), idAndRef.getLeft());
        }
        verify(sidecarsServiceProxy.getYtUpdateTaskController()).insertRows(anyList());
        Assertions.assertEquals(builder.tasks.size(), 1);
    }

    @ParameterizedTest
    @MethodSource("getStatusSource")
    void getStatusTest(Collection<UpdateTaskDTO> startRows, Map<String, String> statuses) {
        SidecarsServiceProxy sidecarsServiceProxy =
                new SidecarsServiceProxyTestBuilder().setStartTable(startRows).setCreatingIssue(createIssue("a",
                        "1")).build();
        for (int i = 0; i < startRows.size(); i++) {
            statuses.put(sidecarsServiceProxy.applyOnPercent(Optional.empty(), OptionalInt.empty(), 10, "test").getLeft(),
                    UpdateTask.Status.IN_PROGRESS.name());
        }
        SidecarsServiceProxyTestBuilder.setGettingStatus(sidecarsServiceProxy.getYtUpdateTaskController(), statuses);
        statuses.forEach((id, status) -> Assertions.assertEquals(status, sidecarsServiceProxy.getStatus(id)));
    }
    static class SidecarsServiceProxyTestBuilder {
        private final Map<String, UpdateTask> tasks = new HashMap<>();
        private final SidecarsService sidecarsService;
        private final StageUpdateNotifier stageUpdateNotifier;
        private final YtUpdateTaskRepository ytUpdateTaskRepository;
        private final UpdateTaskRunner updateTaskRunner;

        public SidecarsServiceProxyTestBuilder() {
            sidecarsService = mock(SidecarsService.class);
            stageUpdateNotifier = mock(StageUpdateNotifier.class);
            ytUpdateTaskRepository = mock(YtUpdateTaskRepository.class);
            updateTaskRunner = mock(UpdateTaskRunner.class);
        }

        public static void setGettingStatus(YtUpdateTaskRepository ytUpdateTaskRepository,
                                            Map<String, String> statuses) {
            when(ytUpdateTaskRepository.lookupRowStatus(any()))
                    .thenAnswer(it -> Optional.ofNullable(statuses.get(it.<String>getArgument(0))));
        }

        public SidecarsServiceProxyTestBuilder setStartTable(Collection<UpdateTaskDTO> table) {
            when(ytUpdateTaskRepository.selectAllRows(any(), any())).thenAnswer(
                    args -> table.stream().map(it -> new UpdateTask(it, args.getArgument(0), args.getArgument(1))).collect(Collectors.toList())
            );
            return this;
        }

        public SidecarsServiceProxyTestBuilder setCreatingIssue(Issue issue) {
            when(stageUpdateNotifier.crateUpdateNotifyMultipleStages(any(), any(), anyInt(), anyString()))
                    .thenReturn(Optional.ofNullable(issue));
            return this;
        }

        public SidecarsServiceProxyTestBuilder setGettingIssue(Issue issue) {
            when(stageUpdateNotifier.getIssue(anyString())).thenReturn(Optional.ofNullable(issue));
            return this;
        }

        public SidecarsServiceProxy build() {
            doAnswer(
                    it -> {
                        UpdateTask task = it.getArgument(0);
                        tasks.put(task.getId(), task);
                        return null;
                    }
            ).when(updateTaskRunner).addTask(any());
            return new SidecarsServiceProxy(sidecarsService, stageUpdateNotifier, updateTaskRunner,
                    ytUpdateTaskRepository);
        }
    }
}
