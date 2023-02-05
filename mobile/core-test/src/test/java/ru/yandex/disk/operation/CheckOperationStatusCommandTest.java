package ru.yandex.disk.operation;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.stubbing.answers.Returns;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.remote.OperationStatus;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;
import ru.yandex.disk.service.CommandScheduler;
import ru.yandex.disk.service.RepeatTimer;
import ru.yandex.disk.service.RepeatableCommandRequest;
import ru.yandex.disk.stats.EventLog;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.trash.ClearTrashOperation;
import ru.yandex.disk.trash.DeleteFromTrashOperation;
import ru.yandex.disk.trash.TrashDatabase;
import ru.yandex.disk.trash.TrashDatabaseOpenHelper;

import java.util.LinkedList;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.mocks.Stubber.stub;

@RunWith(RobolectricTestRunner.class)
public class CheckOperationStatusCommandTest {

    private CheckOperationStatusCommand command;
    private RemoteRepo mockRemoteRepo;
    private EventLogger eventLogger;
    private OperationLists operationLists;
    private CommandScheduler mockCommandScheduler;
    private OperationsFactory opsFactory;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;

        opsFactory = new OperationsFactory(stub(TrashDatabase.class));
        operationLists = TestObjectsFactory.createOperationsDatabase(new TrashDatabaseOpenHelper(context), opsFactory);

        mockRemoteRepo = mock(RemoteRepo.class,
                withSettings().defaultAnswer(new Returns(OperationStatus.OK)));

        eventLogger = new EventLogger();
        mockCommandScheduler = mock(CommandScheduler.class);
        command = new CheckOperationStatusCommand(operationLists, mockRemoteRepo, eventLogger,
                mockCommandScheduler);
    }

    @Test
    public void shouldProcessAllOperations() throws Exception {
        addSentOperation(createClearTrashOperation());
        addSentOperation(createDeleteFromTrashOperation("/some/path"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(getSentOperations(), empty());
        verify(mockCommandScheduler, never()).retry(anyCommandRequest(), anyRepeatTimer());
    }

    @Test
    public void shouldNotifyOperationQueueStateChangedOnce() throws Exception {
        addSentOperation(createClearTrashOperation());
        addSentOperation(createDeleteFromTrashOperation("/some/path"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(eventLogger.getCount(), equalTo(5));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.ClearTrashCompleted.class));
        assertThat(eventLogger.get(1), instanceOf(DiskEvents.DeleteFromTrashCompleted.class));
        assertThat(eventLogger.get(2), instanceOf(DiskEvents.TrashListChanged.class));
        assertThat(eventLogger.get(3), instanceOf(DiskEvents.RemoteTrashListChanged.class));
        assertThat(eventLogger.get(4), instanceOf(DiskEvents.OperationQueueStateChanged.class));
    }

    @Test
    public void shouldNotifyOperationQueueStateChangedOnceOnTwoFails() throws Exception {
        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new PermanentException("test"));

        addSentOperation(createClearTrashOperation());
        addSentOperation(createDeleteFromTrashOperation("/some/path"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThatSendOperationQueueStateChangedEvent();
    }

    @Test
    public void shouldLeaveInQueueIfInProgress() throws Exception {
        Operation operation = createClearTrashOperation();
        operationLists.add(operation, OperationLists.State.SENT);

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(getSentOperations().get(0), equalTo(operation));
    }

    @Test
    public void shouldProcessNextOperationAfterInProgress() throws Exception {
        Operation first = createClearTrashOperation();
        first.setId(100);
        operationLists.add(first, OperationLists.State.SENT);
        addSentOperation(createDeleteFromTrashOperation("/some/path"));

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"))
                .thenReturn(OperationStatus.OK);

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(getSentOperations(), equalTo(asList(first)));
    }

    @Test
    public void shouldNotLoop() throws Exception {
        addSentOperation(createClearTrashOperation());
        addSentOperation(createDeleteFromTrashOperation("/some/path"));

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"))
                .thenReturn(OperationStatus.inProgress("123"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(getSentOperations().size(), equalTo(2));
    }

    @Test
    public void shouldRequestOperationById() throws Exception {
        ClearTrashOperation operation = createClearTrashOperation();
        operation.setStatusCheckId("123");
        addSentOperation(operation);

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"));

        command.execute(new CheckOperationStatusCommandRequest());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockRemoteRepo).getOperationStatus(captor.capture());
        assertThat(captor.getValue(), equalTo("123"));
    }

    @Test
    public void shouldLeaveInQueueOnTemporaryException() throws Exception {
        Operation operation = createClearTrashOperation();
        operationLists.add(operation, OperationLists.State.SENT);

        when(mockRemoteRepo.getOperationStatus(anyString())).
                thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(getSentOperations(), equalTo(asList(operation)));
    }

    @Test
    public void shouldAddToFailedListOnPermanentException() throws Exception {
        Operation operation = createClearTrashOperation();
        operationLists.add(operation, OperationLists.State.SENT);

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new PermanentException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(operationLists.getOperations(OperationLists.State.FAILED),
                equalTo(asList(operation)));
    }

    @Test
    public void shouldNotSendEventThenLeaveInQueue() throws Exception {
        addSentOperation(createClearTrashOperation());

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(eventLogger.getCount(), equalTo(0));
    }

    @Test
    public void shouldNotifyThenMoveToFailsList() throws Exception {
        addSentOperation(createClearTrashOperation());

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new PermanentException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThatSendOperationQueueStateChangedEvent();
    }

    @Test
    public void shouldStopProcessAfterFirstTemporaryException() throws Exception {
        addSentOperation(createClearTrashOperation());
        addSentOperation(createClearTrashOperation());

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        verify(mockRemoteRepo).getOperationStatus(anyString());
    }

    @Test
    public void shouldScheduleRepeatAfterTemporaryError() throws Exception {
        addSentOperation(createClearTrashOperation());
        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new TemporaryException("test"));

        CheckOperationStatusCommandRequest request = new CheckOperationStatusCommandRequest();
        command.execute(request);

        verify(mockCommandScheduler).retry(eq(request), anyRepeatTimer());
    }

    @Test
    public void shouldScheduleRepeatIfHasSentOperation() throws Exception {
        addSentOperation(createClearTrashOperation());
        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"));

        CheckOperationStatusCommandRequest request = new CheckOperationStatusCommandRequest();
        command.execute(request);

        verify(mockCommandScheduler).retry(eq(request), anyRepeatTimer());
    }

    @Test
    public void shouldMoveToFailedAfter100Attempts() throws Exception {
        Operation operation = createClearTrashOperation();
        operation.setAttempts(99);
        operationLists.add(operation, OperationLists.State.SENT);

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        assertThat(operationLists.getOperations(OperationLists.State.SENT), empty());
        assertThat(operationLists.getOperations(OperationLists.State.FAILED),
                equalTo(asList(operation)));
        assertThatSendOperationQueueStateChangedEvent();
    }

    @Test
    public void shouldIncreaseAttemptCounterAfterTemporaryError() throws Exception {
        Operation operation = createClearTrashOperation();
        operation.setAttempts(5);
        operationLists.add(operation, OperationLists.State.SENT);
        addSentOperation(createClearTrashOperation());
        addSentOperation(createClearTrashOperation());

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        LinkedList<Operation> sentOperations = getSentOperations();
        assertThat(sentOperations.get(0).getAttempts(), equalTo(6));
        assertThat(sentOperations.get(1).getAttempts(), equalTo(1));
        assertThat(sentOperations.get(1).getAttempts(), equalTo(1));
    }

    @Test
    public void shouldIncreaseAttemptCounterForInProgressOperation() throws Exception {
        addSentOperation(createClearTrashOperation());

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"));

        command.execute(new CheckOperationStatusCommandRequest());

        LinkedList<Operation> sentOperations = getSentOperations();
        assertThat(sentOperations.get(0).getAttempts(), equalTo(1));
    }

    @Test
    public void shouldNotDoubleIncreaseAttemptCounterForInProgressOperation() throws Exception {
        addSentOperation(createClearTrashOperation());
        addSentOperation(createClearTrashOperation());

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenReturn(OperationStatus.inProgress("123"))
                .thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        LinkedList<Operation> sentOperations = getSentOperations();
        assertThat(sentOperations.get(0).getAttempts(), equalTo(1));
        assertThat(sentOperations.get(1).getAttempts(), equalTo(1));
    }

    @Test
    public void shouldNotRepeatIfQueueEmpty() throws Exception {
        Operation operation = createClearTrashOperation();
        operation.setAttempts(99);
        operationLists.add(operation, OperationLists.State.SENT);

        when(mockRemoteRepo.getOperationStatus(anyString()))
                .thenThrow(new TemporaryException("test"));

        command.execute(new CheckOperationStatusCommandRequest());

        verify(mockCommandScheduler, never()).retry(anyCommandRequest(), anyRepeatTimer());
    }
    
    private ClearTrashOperation createClearTrashOperation() {
        final ClearTrashOperation operation = opsFactory.createClearTrashOperation();
        operation.setStatusCheckId("statusCheckId");
        return operation;
    }

    private DeleteFromTrashOperation createDeleteFromTrashOperation(final String path) {
        final DeleteFromTrashOperation operation = opsFactory.createDeleteFromTrashOperation(path);
        operation.setStatusCheckId("statusCheckId");
        return operation;
    }

    private LinkedList<Operation> getSentOperations() {
        return operationLists.getOperations(OperationLists.State.SENT);
    }

    private void addSentOperation(Operation operation) {
        operationLists.add(operation, OperationLists.State.SENT);
    }

    private void assertThatSendOperationQueueStateChangedEvent() {
        assertThat(eventLogger.getCount(), equalTo(1));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.OperationQueueStateChanged.class));
    }

    private static RepeatTimer anyRepeatTimer() {
        return any(RepeatTimer.class);
    }

    private static RepeatableCommandRequest anyCommandRequest() {
        return any(RepeatableCommandRequest.class);
    }

}
