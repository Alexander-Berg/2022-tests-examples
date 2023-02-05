package ru.yandex.disk.operation;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.stubbing.answers.Returns;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.remote.OperationStatus;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.trash.DeleteFromTrashOperation;
import ru.yandex.disk.trash.TrashDatabase;
import ru.yandex.disk.trash.TrashDatabaseOpenHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.mocks.Stubber.stub;

@RunWith(RobolectricTestRunner.class)
public class PushOperationsCommandTest {

    private EventLogger eventLogger;
    private RemoteRepo mockRemoteRepo;

    private PushOperationsCommand command;
    private CommandLogger commandLogger;
    private OperationLists operationLists;
    private OperationsFactory opsFactory;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;
        opsFactory = new OperationsFactory(stub(TrashDatabase.class));
        operationLists = TestObjectsFactory.createOperationsDatabase(new TrashDatabaseOpenHelper(context), opsFactory);

        mockRemoteRepo = mock(RemoteRepo.class,
                withSettings().defaultAnswer(new Returns(OperationStatus.OK)));
        eventLogger = new EventLogger();
        commandLogger = new CommandLogger();
        command = new PushOperationsCommand(operationLists, mockRemoteRepo, eventLogger,
                commandLogger);
    }

    @Test
    public void shouldProcessAllOperations() throws Exception {
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);
        operationLists.add(new DeleteFromTrashOperation(null, "/some/path"),
                OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(operationLists.getOperations(OperationLists.State.IN_QUEUE), empty());
        assertThat(operationLists.getOperations(OperationLists.State.SENT), empty());
        assertThat(operationLists.getOperations(OperationLists.State.FAILED), empty());

        assertThat(commandLogger.getCount(), equalTo(1));
        assertThat(commandLogger.get(0), instanceOf(CheckOperationStatusCommandRequest.class));
    }

    @Test
    public void shouldSendEventsOnSuccess() throws Exception {
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(eventLogger.getCount(), equalTo(4));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.ClearTrashCompleted.class));
        assertThat(eventLogger.get(1), instanceOf(DiskEvents.TrashListChanged.class));
        assertThat(eventLogger.get(2), instanceOf(DiskEvents.RemoteTrashListChanged.class));
        assertThat(eventLogger.get(3), instanceOf(DiskEvents.OperationQueueStateChanged.class));
    }

    @Test
    public void TrashListChangedOnce() throws Exception {
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(eventLogger.getCount(), equalTo(5));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.ClearTrashCompleted.class));
        assertThat(eventLogger.get(1), instanceOf(DiskEvents.ClearTrashCompleted.class));
        assertThat(eventLogger.get(2), instanceOf(DiskEvents.TrashListChanged.class));
        assertThat(eventLogger.get(3), instanceOf(DiskEvents.RemoteTrashListChanged.class));
        assertThat(eventLogger.get(4), instanceOf(DiskEvents.OperationQueueStateChanged.class));
    }

    @Test
    public void shouldAddToFailsListIfTemporaryException() throws Exception {
        when(mockRemoteRepo.clearTrash()).thenThrow(new TemporaryException("test"));

        Operation operation = opsFactory.createClearTrashOperation();
        operation.setId(100);
        operationLists.add(operation, OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(eventLogger.getCount(), equalTo(1));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.OperationQueueStateChanged.class));
        assertThat(operationLists.getOperations(OperationLists.State.IN_QUEUE), empty());
        assertThat(operationLists.getOperations(OperationLists.State.FAILED).get(0),
                equalTo(operation));
    }

    @Test
    public void shouldAddToSecondaryQueueIfInProgressStatusWasReceived() throws Exception {
        when(mockRemoteRepo.clearTrash()).thenReturn(OperationStatus.inProgress("123"));

        Operation operation = opsFactory.createClearTrashOperation();
        operationLists.add(operation, OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(eventLogger.getCount(), equalTo(1));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.OperationQueueStateChanged.class));
        assertThat(operationLists.getOperations(OperationLists.State.IN_QUEUE), empty());
        Operation sentOperation = operationLists.getOperations(OperationLists.State.SENT).get(0);
        assertThat(sentOperation, equalTo(operation));
        assertThat(sentOperation.getStatusCheckId(), equalTo("123"));
        assertThat(operationLists.getOperations(OperationLists.State.FAILED), empty());
    }

    @Test
    public void shouldStartCheckOperationStatusCommandIfInProgressStatusWasReceived() throws Exception {
        when(mockRemoteRepo.clearTrash()).thenReturn(OperationStatus.inProgress("123"));

        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(commandLogger.get(0), instanceOf(CheckOperationStatusCommandRequest.class));
    }

    @Test
    public void shouldStartCheckOperationStatusIsSentOnce() throws Exception {
        when(mockRemoteRepo.clearTrash()).thenReturn(OperationStatus.inProgress("123"));

        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);
        operationLists.add(new DeleteFromTrashOperation(null, "/some/path"),
                OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(commandLogger.getCount(), equalTo(1));
        assertThat(commandLogger.get(0), instanceOf(CheckOperationStatusCommandRequest.class));
    }

    @Test
    public void shouldFailAllOperationAfterFirstTemporaryException() throws Exception {
        when(mockRemoteRepo.clearTrash())
                .thenReturn(OperationStatus.inProgress("123"))
                .thenThrow(new TemporaryException("test"));

        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        verify(mockRemoteRepo, times(2)).clearTrash();
        assertThat(operationLists.getOperations(OperationLists.State.IN_QUEUE), empty());
        assertThat(operationLists.getOperations(OperationLists.State.SENT).size(), equalTo(1));
        assertThat(operationLists.getOperations(OperationLists.State.FAILED).size(), equalTo(2));

    }

    @Test
    public void shouldProcessNextOperationAfterPermanentException() throws Exception {
        when(mockRemoteRepo.clearTrash())
                .thenThrow(new PermanentException("test"))
                .thenReturn(OperationStatus.OK);

        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);
        operationLists.add(opsFactory.createClearTrashOperation(), OperationLists.State.IN_QUEUE);

        command.execute(new PushOperationsCommandRequest());

        assertThat(operationLists.getOperations(OperationLists.State.IN_QUEUE), empty());
        assertThat(operationLists.getOperations(OperationLists.State.FAILED).size(), equalTo(1));
    }

    @Test
    public void shouldProcessEmptyQueue() throws Exception {
        command.execute(new PushOperationsCommandRequest());

        assertThat(commandLogger.getCount(), equalTo(1));
        assertThat(eventLogger.getAll(), empty());

    }
}
