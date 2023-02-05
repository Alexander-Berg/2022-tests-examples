package ru.yandex.disk.operation;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.trash.ClearTrashOperation;
import ru.yandex.disk.trash.TrashDatabaseOpenHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(RobolectricTestRunner.class)
public class AddToOperationQueueCommandTest {
    private CommandLogger commandLogger;
    private AddToOperationQueueCommand command;
    private EventLogger eventLogger;
    private OperationLists operationLists;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;
        operationLists = TestObjectsFactory.createOperationsDatabase(new TrashDatabaseOpenHelper(context), new OperationsFactory(null));
        eventLogger = new EventLogger();
        commandLogger = new CommandLogger();
        command = new AddToOperationQueueCommand(operationLists, eventLogger, commandLogger);
    }

    @Test
    public void shouldAddOperationToQueue() throws Exception {
        ClearTrashOperation operation = new ClearTrashOperation(null);
        command.execute(new AddToOperationQueueCommandRequest(operation));

        assertThat(eventLogger.getCount(), equalTo(1));
        assertThat(eventLogger.get(0), instanceOf(DiskEvents.OperationQueueStateChanged.class));
        assertThat(operation, equalTo(operationLists.getOperations(OperationLists.State.IN_QUEUE).get(0)));
    }

    @Test
    public void shouldStatPushOperationsCommand() throws Exception {
        command.execute(new AddToOperationQueueCommandRequest(new ClearTrashOperation(null)));

        assertThat(commandLogger.get(0), instanceOf(PushOperationsCommandRequest.class));
    }
}
