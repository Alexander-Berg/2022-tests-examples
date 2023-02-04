package ru.yandex.infra.stage.deployunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.stage.StageContext;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.DeployProgress;
import ru.yandex.infra.stage.primitives.DeployPrimitiveStatus;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;
import static ru.yandex.infra.stage.util.AssertUtils.assertCollectionMatched;

public class SequentialControllerTest {

    private static final StageContext STAGE_CONTEXT_WITH_EMPTY_ACL = TestData.DEFAULT_STAGE_CONTEXT.withAcl(
            new Acl(Collections.emptyList())
    );

    @Test
    public void raceConditionForDoubleSentState() {
        MultiplexingController.Factory<String, ReadyStatus> factory = mock(MultiplexingController.Factory.class);
        ObjectController<String, ReadyStatus> oneShotObjectConsumer = mock(ObjectController.class);
        when(factory.createController(anyString(), anyString(), any(Consumer.class))).thenReturn(oneShotObjectConsumer);

        AtomicReference<Pair<String, String>> objectResult = new AtomicReference<>(null);
        doAnswer((v) -> {
            objectResult.set(Pair.of(v.getArgument(0, String.class), v.getArgument(3, String.class)));
            return null;
        }).when(oneShotObjectConsumer).sync(anyString(), any(StageContext.class), anyMap(), anyString());
        doReturn((ReadyStatus) (() -> true)).when(oneShotObjectConsumer).getStatus();

        ExecutorService executorService = mock(ExecutorService.class);
        Queue<CompletableFuture<?>> tasks = new LinkedList<>();
        when(executorService.submit(Mockito.any(Runnable.class))).thenAnswer((v) -> {
            final CompletableFuture<Object> future = new CompletableFuture<>();
            tasks.add(future);
            return future.thenRun(v.getArgument(0, Runnable.class));
        });
        SequentialController<String, ReadyStatus> sequentialController = new SequentialController<>("ID", factory,
                (v) -> {
                }, executorService);
        HashMap<String, String> specs = new HashMap<>();
        specs.put("sas", "sas-spec");
        specs.put("vla", "vla-spec");
        List<Pair<String, Boolean>> sequence = new ArrayList<>();
        sequence.add(Pair.of("sas", false));
        sequence.add(Pair.of("vla", true));
        sequentialController.sync(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, Collections.emptySet(),
                Collections.emptyMap(), sequence);
        assertThat(tasks, not(empty()));
        tasks.poll().complete(null);
        assertThat(objectResult.get().getValue(), equalTo("sas"));
        assertThat(objectResult.get().getKey(), equalTo("sas-spec"));

        objectResult.set(null);
        sequentialController.applyNext(() -> true);
        assertThat(tasks, not(empty()));
        tasks.poll().complete(null);
        assertThat(objectResult.get(), equalTo(null));//vla is not approved yet

        sequentialController.sync(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, Collections.singleton("vla"),
                Collections.emptyMap(), sequence);
        assertThat(tasks, not(empty()));
        tasks.poll().complete(null);

        assertThat(objectResult.get().getValue(), equalTo("vla"));
        assertThat(objectResult.get().getKey(), equalTo("vla-spec"));
    }

    @Test
    public void dontLetToUseOneForAnotherDataTest() {
        MultiplexingController.Factory<String, ReadyStatus> factory = mock(MultiplexingController.Factory.class);
        ExecutorService executorService = mock(ExecutorService.class);
        Queue<Pair<CompletableFuture<?>, CompletableFuture<?>>> tasks = new LinkedList<>();
        when(executorService.submit(Mockito.any(Runnable.class))).thenAnswer((v) -> {
            final CompletableFuture<Object> future = new CompletableFuture<>();
            final CompletableFuture<Void> result = future.thenRun(v.getArgument(0, Runnable.class));
            tasks.add(Pair.of(future, result));
            return result;
        });
        SequentialController<String, ReadyStatus> sequentialController = new SequentialController<>("ID", factory,
                (v) -> {
                }, executorService);
        HashMap<String, String> specs = new HashMap<>();
        specs.put("sas", "sas-spec");
        specs.put("vla", "vla-spec");
        List<Pair<String, Boolean>> sequence = new ArrayList<>();
        sequence.add(Pair.of("sas", false));
        sequence.add(Pair.of("vla", true));
        assertThat(sequentialController.isEquals(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, Collections.emptyMap(), sequence), equalTo(true));

        sequentialController.sync(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, Collections.emptySet(),
                Collections.emptyMap(), sequence);
        tasks.poll().getLeft().complete(null);

        List<Pair<String, Boolean>> sequenceTwo = new ArrayList<>();
        sequence.add(Pair.of("sas", false));
        sequence.add(Pair.of("vla", false));

        assertThat(sequentialController.isEquals(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, Collections.emptyMap(), sequenceTwo),
                equalTo(false));

        sequentialController.sync(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, Collections.emptySet(),
                Collections.emptyMap(), sequenceTwo);

        final Pair<CompletableFuture<?>, CompletableFuture<?>> lastTask = tasks.poll();
        lastTask.getLeft().complete(null);
        assertThrows(IllegalStateException.class, () -> get1s(lastTask.getRight()));
    }


    private Set<String> buildApproveSet(TreeMap<String, Boolean> locations) {
        return EntryStream.of(locations).filterValues(v -> v).keys().toSet();
    }

    @Test
    public void collectStatusTest() {
        var locationsApproves = new TreeMap<>(Map.of(
                "sas", false,
                "vla", false
        ));

        var specs = ImmutableMap.of(
                "sas", TestData.REPLICA_SET_SPEC,
                "vla", TestData.REPLICA_SET_SPEC
        );

        var inProgressStatus =
                new DeployPrimitiveStatus<TReplicaSetStatus>(Readiness.inProgress("REPLICA_SET_OUT_OF_SYNC"), new DeployProgress(0, 0, 1), Optional.empty());

        var readyStatus =
                new DeployPrimitiveStatus<TReplicaSetStatus>(Readiness.ready(), new DeployProgress(1, 0, 1), Optional.empty());

        var sasController = mock(ObjectController.class);
        var vlaController = mock(ObjectController.class);
        when(sasController.getStatus()).thenAnswer(s -> locationsApproves.get("sas") ? readyStatus : inProgressStatus);
        when(vlaController.getStatus()).thenAnswer(s -> locationsApproves.get("vla") ? readyStatus : inProgressStatus);

        MultiplexingController.Factory<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> factory = mock(MultiplexingController.Factory.class);

        when(factory.createController(anyString(), anyString(), any(Consumer.class))).thenAnswer((a) -> {
            if (a.getArgument(1).equals("sas")) {
                return sasController;
            }
            if (a.getArgument(1).equals("vla")) {
                return vlaController;
            }
            return null;
        });

        var executorService = mock(ExecutorService.class);
        Queue<Pair<CompletableFuture<?>, CompletableFuture<?>>> tasks = new LinkedList<>();
        when(executorService.submit(any(Runnable.class))).thenAnswer(runnable -> {
            var future = new CompletableFuture<>();
            var result = future.thenRun(runnable.getArgument(0, Runnable.class));
            tasks.add(Pair.of(future, result));
            return result;
        });

        SequentialController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> sequentialController = new SequentialController<>("ID", factory,
                (v) -> {
                }, executorService);


        var sequence = ImmutableList.of(
                Pair.of("sas", true),
                Pair.of("vla", true)
        );

        // sync with one approve
        locationsApproves.put("sas", true);
        sequentialController.sync(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, buildApproveSet(locationsApproves), Collections.emptyMap(), sequence);

        var lastTask = tasks.poll();
        lastTask.getLeft().complete(null);
        get1s(lastTask.getRight());

        var statuses = sequentialController.getClusterStatuses();
        assertThat(statuses.size(), equalTo(2));
        assertThat(statuses.get("sas").getReadiness(), equalTo(Readiness.ready()));
        assertThat(statuses.get("vla").getReadiness(), not(Readiness.ready()));

        // sync with two approves
        locationsApproves.put("vla", true);
        sequentialController.sync(specs, STAGE_CONTEXT_WITH_EMPTY_ACL, buildApproveSet(locationsApproves), Collections.emptyMap(), sequence);

        lastTask = tasks.poll();
        lastTask.getLeft().complete(null);
        get1s(lastTask.getRight());

        statuses = sequentialController.getClusterStatuses();
        assertCollectionMatched(statuses.values(), 2, s -> s.getReadiness().equals(Readiness.ready()));
    }
}
