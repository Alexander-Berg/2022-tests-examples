package ru.yandex.infra.controller.yp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayload;
import ru.yandex.yp.model.YpPayloadFormat;
import ru.yandex.yp.model.YpSelectStatement;
import ru.yandex.yp.model.YpSelectedObjects;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

public class YpRequestWithPagingTest {

    private YpRawObjectService ypRawObjectService;
    private YpSelectStatement.Builder builder;

    @BeforeEach
    void before() {
        ypRawObjectService = Mockito.mock(YpRawObjectService.class);
        builder = YpSelectStatement.builder(YpObjectType.STAGE, YpPayloadFormat.YSON)
                .addSelector(Paths.ID);
    }

    private Integer deserialize(List<YpPayload> payloads) {
        return null;
    }

    @Test
    void selectObjectsTest() {

        int expectedObjectsCount = 13;
        AtomicInteger processedObjects = new AtomicInteger(0);
        AtomicInteger callsCount = new AtomicInteger(0);
        int pageSize = 5;
        List<String> continuationTokens = new ArrayList<>();

        Mockito.doAnswer(v -> {
            callsCount.incrementAndGet();

            YpSelectStatement statement = v.getArgument(0, YpSelectStatement.class);
            assertEquals(pageSize, statement.getLimit().orElseThrow());
            continuationTokens.add(statement.getContinuationToken().orElse(""));

            var resultSize = Math.min(pageSize, expectedObjectsCount - processedObjects.get());
            List<Integer> range = IntStream.rangeClosed(processedObjects.get(), processedObjects.get() + resultSize - 1).boxed().collect(Collectors.toList());
            processedObjects.addAndGet(resultSize);
            return completedFuture(new YpSelectedObjects<>(range, 0, String.valueOf(callsCount.get())));
        }).when(ypRawObjectService).selectObjects(any(), any());

        List<Integer> expectedResult = IntStream.rangeClosed(0, expectedObjectsCount - 1).boxed().collect(Collectors.toList());
        List<Integer> result = get1s(YpRequestWithPaging.selectObjects(ypRawObjectService, pageSize, builder, this::deserialize));
        assertEquals(expectedResult, result);
        assertEquals(3, callsCount.get());
        assertEquals(List.of("", "1", "2"), continuationTokens);
    }

    @Test
    void stopOnAnyExceptionExceptStatusRuntimeException() {

        AtomicInteger callsCount = new AtomicInteger(0);

        Mockito.doAnswer(v -> {
            callsCount.incrementAndGet();
            return failedFuture(new RuntimeException("Failed"));
        }).when(ypRawObjectService).selectObjects(any(), any());

        assertThrows(RuntimeException.class, () -> get1s(YpRequestWithPaging.selectObjects(ypRawObjectService, 10, builder, this::deserialize)));
        assertEquals(1, callsCount.get());
    }

    @Test
    void reducePageSizeAfterSomeFailures() {

        int numberOfFailures = 2;
        AtomicInteger callsCount = new AtomicInteger(0);

        List<Long> pageSizes = new ArrayList<>();
        List<Integer> expectedResult = List.of(1, 2, 3);

        Mockito.doAnswer(v -> {
            YpSelectStatement statement = v.getArgument(0, YpSelectStatement.class);
            pageSizes.add(statement.getLimit().orElseThrow());

            if (callsCount.incrementAndGet() <= numberOfFailures) {
                return failedFuture(new StatusRuntimeException(Status.RESOURCE_EXHAUSTED));
            }

            return completedFuture(new YpSelectedObjects<>(expectedResult, 0, "continuation token"));
        }).when(ypRawObjectService).selectObjects(any(), any());

        List<Integer> result = get1s(YpRequestWithPaging.selectObjects(ypRawObjectService, 100, builder, this::deserialize));
        assertEquals(expectedResult, result);
        assertEquals(List.of(100L, 50L, 25L), pageSizes);
        assertEquals(3, callsCount.get());
    }

    @Test
    void restorePageSizeAfterSuccess() {

        int numberOfFailures = 2;
        int stopAfter = 5;
        AtomicInteger callsCount = new AtomicInteger(0);

        int pageSize = 100;
        List<Long> pageSizes = new ArrayList<>();

        Mockito.doAnswer(v -> {
            YpSelectStatement statement = v.getArgument(0, YpSelectStatement.class);
            pageSizes.add(statement.getLimit().orElseThrow());

            if (callsCount.incrementAndGet() <= numberOfFailures) {
                return failedFuture(new StatusRuntimeException(Status.RESOURCE_EXHAUSTED));
            }

            List<Integer> result = callsCount.get() <= stopAfter ?
                    IntStream.rangeClosed(1, pageSize).boxed().collect(Collectors.toList()) :
                    Collections.emptyList();
            return completedFuture(new YpSelectedObjects<>(result, 0, "continuation token"));
        }).when(ypRawObjectService).selectObjects(any(), any());

        List<Integer> result = get1s(YpRequestWithPaging.selectObjects(ypRawObjectService, pageSize, builder, this::deserialize));
        assertEquals((stopAfter - numberOfFailures)*pageSize, result.size());
        assertEquals(List.of(100L, 50L, 25L, 50L, 100L, 100L), pageSizes);
        assertEquals(6, callsCount.get());
    }

    @Test
    void failCompletelyWhenPageSizeGoesTo1() {
        List<Long> pageSizes = new ArrayList<>();

        Mockito.doAnswer(v -> {
            YpSelectStatement statement = v.getArgument(0, YpSelectStatement.class);
            pageSizes.add(statement.getLimit().orElseThrow());
            return failedFuture(new StatusRuntimeException(Status.RESOURCE_EXHAUSTED));
        }).when(ypRawObjectService).selectObjects(any(), any());

        assertThrows(ObjectsPaginationExcecutorException.class, () -> get1s(YpRequestWithPaging.selectObjects(ypRawObjectService, 10, builder, this::deserialize)));
        assertEquals(List.of(10L, 5L, 2L, 1L), pageSizes);
    }

}
