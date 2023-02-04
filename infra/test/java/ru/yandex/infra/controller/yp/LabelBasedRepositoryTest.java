package ru.yandex.infra.controller.yp;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;

import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.Autogen;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;
import ru.yandex.yp.model.YpError;
import ru.yandex.yp.model.YpEvent;
import ru.yandex.yp.model.YpEventType;
import ru.yandex.yp.model.YpException;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayload;
import ru.yandex.yp.model.YpSelectStatement;
import ru.yandex.yp.model.YpSelectedObjects;
import ru.yandex.yp.model.YpTypedId;
import ru.yandex.yp.model.YpWatchObjectsStatement;
import ru.yandex.yp.model.YpWatchedObjects;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;
import static ru.yandex.yp.model.YpErrorCodes.ROWS_ALREADY_TRIMMED;

public class LabelBasedRepositoryTest {
    private static final ObjectBuilderDescriptor<Autogen.TSchemaMeta, SchemaMeta> DESCRIPTOR = new ObjectBuilderDescriptor<>(
            TReplicaSetSpec::newBuilder, TReplicaSetStatus::newBuilder, SchemaMeta::fromProto, Autogen.TSchemaMeta.getDefaultInstance());
    private static final int PAGE_SIZE = 10;
    private static final Selector SELECTOR = new Selector.Builder().build();
    private static final Long TIMESTAMP = 123L;

    private YpRawObjectService objectService;
    private LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> repository;
    static final GaugeRegistry gaugeRegistry = GaugeRegistry.EMPTY;

    @BeforeEach
    void before() {
        objectService = Mockito.mock(YpRawObjectService.class);
        repository = new LabelBasedRepository<>(YpObjectType.REPLICA_SET, Collections.emptyMap(), Optional.empty(),
                objectService, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);
    }

    @Test
    void lowerLimitOnFailureWhileListAndSelectObjects() {
        doReturn(completedFuture(TIMESTAMP)).when(objectService).generateTimestamp();
        doReturn(grpcFailedFuture(Status.RESOURCE_EXHAUSTED)).when(objectService).selectObjects(selectHasPageSize(PAGE_SIZE), any());
        doReturn(completedFuture(new YpSelectedObjects<>(ImmutableList.of(singleObject("1")), 0L)))
                .when(objectService).selectObjects(selectHasPageSize(PAGE_SIZE / 2), any());
        get1s(repository.selectObjects(SELECTOR, Collections.emptyMap()));
    }

    @Test
    void lowerLimitOnFailureWhileWatchObjects() {
        doReturn(completedFuture(0L)).when(objectService).generateTimestamp();
        doReturn(grpcFailedFuture(Status.RESOURCE_EXHAUSTED)).when(objectService).watchObjects(watchHasPageSize(PAGE_SIZE));
        doReturn(completedFuture(new YpWatchedObjects(eventList(0, 1), 0L)))
                .when(objectService).watchObjects(watchHasPageSize(PAGE_SIZE / 2));
        get1s(repository.watchObjects(0L, Duration.ofSeconds(1)));
    }

    @Test
    void exceptionOnFailureOfRowsTrimmedWhileWatchAndSelectObjects() {
        doReturn(completedFuture(0L)).when(objectService).generateTimestamp();
        doReturn(ypFailedFuture(ROWS_ALREADY_TRIMMED)).when(objectService).watchObjects(watchHasPageSize(PAGE_SIZE));
        assertThrows(Exception.class, () -> get1s(repository.watchObjects(0L, Duration.ofSeconds(1))));

        doReturn(ypFailedFuture(ROWS_ALREADY_TRIMMED)).when(objectService).selectObjects(selectHasPageSize(PAGE_SIZE), any());
        assertThrows(Exception.class, () -> get1s(repository.selectObjects(SELECTOR, Collections.emptyMap())));
    }

    @Test
    void completeExceptionallyOnFailingAllSizesWhileListObjects() {
        doReturn(completedFuture(TIMESTAMP)).when(objectService).generateTimestamp();
        doReturn(grpcFailedFuture(Status.RESOURCE_EXHAUSTED)).when(objectService).selectObjects(any(), any());
        assertThrows(Exception.class, () -> get1s(repository.selectObjects(SELECTOR, Collections.emptyMap())));
        verify(objectService).selectObjects(selectHasPageSize(1), any());
    }

    @Test
    void completeExceptionallyOnFailingAllSizesWhileWatchObjects() {
        doReturn(completedFuture(TIMESTAMP)).when(objectService).generateTimestamp();
        doReturn(grpcFailedFuture(Status.RESOURCE_EXHAUSTED)).when(objectService).watchObjects(any());
        assertThrows(Exception.class, () -> get1s(repository.watchObjects(0L, Duration.ofSeconds(1))));
        verify(objectService).watchObjects(watchHasPageSize(1));
    }

    @Test
    void restoreLimitForNextPagesWhileListAndSelectObjects() {
        doReturn(completedFuture(TIMESTAMP)).when(objectService).generateTimestamp();
        int secondResponseCount = PAGE_SIZE - 1;
        AtomicBoolean isFirst = new AtomicBoolean(true);
        Stubber answer = doAnswer(invocation -> {
            if (isFirst.get()) {
                isFirst.set(false);
                return grpcFailedFuture(Status.RESOURCE_EXHAUSTED);
            } else {
                return completedFuture(new YpSelectedObjects<>(objectList(PAGE_SIZE / 2, secondResponseCount), 0L));
            }
        });

        answer.when(objectService).selectObjects(selectHasPageSize(PAGE_SIZE), any());
        doReturn(completedFuture(new YpSelectedObjects<>(objectList(0, PAGE_SIZE / 2), 0L)))
                .when(objectService).selectObjects(selectHasPageSize(PAGE_SIZE / 2), any());
        SelectedObjects<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> selectResult =
                get1s(repository.selectObjects(SELECTOR, Collections.emptyMap()));
        assertThat(selectResult.getObjects().entrySet(), hasSize(PAGE_SIZE / 2 + secondResponseCount));
        assertThat(selectResult.getTimestamp(), equalTo(TIMESTAMP));
    }

    @Test
    void restoreLimitForNextPagesWhileWatchObjects() {
        doReturn(completedFuture(TIMESTAMP)).when(objectService).generateTimestamp();
        int secondResponseCount = PAGE_SIZE - 1;
        AtomicBoolean isFirst = new AtomicBoolean(true);
        Stubber answer = doAnswer(invocation -> {
            if (isFirst.get()) {
                isFirst.set(false);
                return grpcFailedFuture(Status.RESOURCE_EXHAUSTED);
            } else {
                return completedFuture(new YpWatchedObjects(eventList(PAGE_SIZE / 2, secondResponseCount), 0L));
            }
        });

        answer.when(objectService).watchObjects(watchHasPageSize(PAGE_SIZE));
        doReturn(completedFuture(new YpWatchedObjects(eventList(0, PAGE_SIZE / 2), 0)))
                .when(objectService).watchObjects(watchHasPageSize(PAGE_SIZE / 2));
        WatchedObjects watchResult = get1s(repository.watchObjects(0L, Duration.ofSeconds(1)));
        assertThat(watchResult.getEvents().entrySet(), hasSize(PAGE_SIZE / 2 + secondResponseCount));
        assertThat(watchResult.getTimestamp(), equalTo(TIMESTAMP));
    }

    private CompletableFuture<?> ypFailedFuture(int errorCode) {
        return failedFuture(new YpException("very message", "much description", new YpError(errorCode,
                "better message", Collections.emptyMap(), Collections.emptyList()), null));
    }

    private CompletableFuture<?> grpcFailedFuture(Status status) {
        return failedFuture(new StatusRuntimeException(status));
    }

    private static YpSelectStatement selectHasPageSize(int pageSize) {
        return argThat(argument -> argument.getLimit().map(value -> value.intValue() == pageSize).orElse(false));
    }

    private static YpWatchObjectsStatement watchHasPageSize(int pageSize) {
        return argThat(argument -> argument.getEventCountLimit().map(value -> value.intValue() == pageSize).orElse(false));
    }

    private List<YpPayload> singleObject(String id) {
        return ImmutableList.of(
                YpPayload.yson(ByteString.copyFrom(new YTreeBuilder().value(id).build().toBinary()))
        );
    }

    private List<List<YpPayload>> objectList(int start, int count) {
        return IntStream.range(start, start + count)
                .mapToObj(item -> singleObject(String.valueOf(item)))
                .collect(Collectors.toList());
    }

    private List<YpEvent> eventList(int start, int count) {
        return IntStream.range(start, start + count)
                .mapToObj(item -> new YpEvent(0, YpEventType.CREATED, new YpTypedId("id" + item, YpObjectType.STAGE)))
                .collect(Collectors.toList());
    }
}
