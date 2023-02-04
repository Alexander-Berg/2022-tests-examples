package ru.yandex.infra.controller.yp;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.model.YpObjectHistory;
import ru.yandex.yp.model.YpObjectHistoryEvent;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpObjectUpdate;
import ru.yandex.yp.model.YpSelectedObjects;
import ru.yandex.yp.model.YpTransaction;
import ru.yandex.yp.model.YpTypedId;
import ru.yandex.yp.model.YpTypedObject;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

class ReadonlyYpRawObjectServiceTest {

    private YpRawObjectService objectService;
    private ReadonlyYpRawObjectService readonlyYp;

    @BeforeEach
    void before() {
        objectService = Mockito.mock(YpRawObjectService.class);
        readonlyYp = new ReadonlyYpRawObjectService(objectService);
    }

    @Test
    void shouldProcessReadOperations() {

        doReturn(completedFuture(15L)).when(objectService).generateTimestamp();
        assertThat(get1s(readonlyYp.generateTimestamp()), equalTo(15L));

        YpTransaction transaction = new YpTransaction("id", 545L, 0);
        doReturn(completedFuture(transaction)).when(objectService).startTransaction();
        assertThat(get1s(readonlyYp.startTransaction()), sameInstance(transaction));

        doReturn(completedFuture(16L)).when(objectService).commitTransaction(any());
        assertThat(get1s(readonlyYp.commitTransaction(transaction)), equalTo(16L));

        doReturn(completedFuture(null)).when(objectService).abortTransaction(any());
        get1s(readonlyYp.abortTransaction(transaction));
        verify(objectService, times(1)).abortTransaction(any());

        doReturn(completedFuture("result")).when(objectService).getObject(any(), any());
        assertThat(get1s(readonlyYp.getObject(null, x -> "")), equalTo("result"));

        doReturn(completedFuture(List.of("result2"))).when(objectService).getObjects(any(), any());
        assertThat(get1s(readonlyYp.getObjects(null, x -> List.of(""))), equalTo(List.of("result2")));

        YpSelectedObjects<String> selectedObjects = new YpSelectedObjects<>(List.of("value"), 15L);
        doReturn(completedFuture(selectedObjects)).when(objectService).selectObjects(any(), any());
        assertThat(get1s(readonlyYp.selectObjects(null, x -> "")), equalTo(selectedObjects));

        YpObjectHistory<YpObjectHistoryEvent<String>> history = new YpObjectHistory<>(List.of(), "token");
        doReturn(completedFuture(history)).when(objectService).selectObjectHistory(any(), any());
        assertThat(get1s(readonlyYp.selectObjectHistory(null, x -> "")), sameInstance(history));
    }

    @Test
    void shouldSkipWriteOperations() {
        assertThat(get1s(readonlyYp.createObject(new YpTypedObject(YpObjectType.STAGE, null))), notNullValue());
        verify(objectService, times(0)).createObject(any());

        YpTransaction transaction = new YpTransaction("id", 545L, 0);
        assertThat(get1s(readonlyYp.createObject(new YpTypedObject(YpObjectType.STAGE, null), transaction)),
                notNullValue());
        verify(objectService, times(0)).createObject(any(), any());

        assertThat(get1s(readonlyYp.createObjects(Collections.emptyList())), equalTo(Collections.emptyList()));
        verify(objectService, times(0)).createObjects(any());

        assertThat(get1s(readonlyYp.createObjects(Collections.emptyList(), transaction)), equalTo(Collections.emptyList()));
        verify(objectService, times(0)).createObjects(any(), any());

        var id = new YpTypedId("id", YpObjectType.STAGE);
        get1s(readonlyYp.removeObject(id));
        verify(objectService, times(0)).removeObject(any());

        get1s(readonlyYp.removeObject(id, transaction));
        verify(objectService, times(0)).removeObject(any(), any());

        get1s(readonlyYp.removeObjects(List.of(id)));
        verify(objectService, times(0)).removeObjects(any());

        get1s(readonlyYp.removeObjects(List.of(id), transaction));
        verify(objectService, times(0)).removeObjects(any(), any());

        var update = YpObjectUpdate.builder(id).build();
        get1s(readonlyYp.updateObject(update));
        verify(objectService, times(0)).updateObject(any());

        get1s(readonlyYp.updateObject(update, transaction));
        verify(objectService, times(0)).updateObject(any(), any());

        get1s(readonlyYp.updateObjects(List.of(update)));
        verify(objectService, times(0)).updateObjects(any());

        get1s(readonlyYp.updateObjects(List.of(update), transaction));
        verify(objectService, times(0)).updateObjects(any(), any());
    }
}
