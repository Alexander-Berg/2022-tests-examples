package ru.yandex.disk.operation;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.trash.ClearTrashOperation;
import ru.yandex.disk.trash.DeleteFromTrashOperation;
import ru.yandex.disk.trash.TrashDatabaseOpenHelper;

import java.util.LinkedList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(RobolectricTestRunner.class)
public class OperationsDatabaseTest {

    private OperationsDatabase database;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;
        database = TestObjectsFactory.createOperationsDatabase(new TrashDatabaseOpenHelper(context), new OperationsFactory(null));
    }

    @Test
    public void shouldSaveAllFields() throws Exception {
        Operation operation = new DeleteFromTrashOperation(null, "path/to/file");
        operation.setId(100);
        operation.setStatusCheckId("123");

        database.add(operation, OperationLists.State.IN_QUEUE);

        Operation restored = database.getOperations(OperationLists.State.IN_QUEUE).get(0);
        assertThat(restored, notNullValue());

        assertThat(restored.getPath(), equalTo("path/to/file"));
        assertThat(restored.getStatusCheckId(), equalTo("123"));
        assertThat(restored.getId(), equalTo(100L));
    }

    @Test
    public void shouldInstantiateCorrectClass() throws Exception {
        database.add(new DeleteFromTrashOperation(null, "path/to/file"), OperationLists.State.IN_QUEUE);

        assertThat(database.getOperations(OperationLists.State.IN_QUEUE).get(0),
                instanceOf(DeleteFromTrashOperation.class));

        database.add(getClearTrashOperation(), OperationLists.State.SENT);

        assertThat(database.getOperations(OperationLists.State.SENT).get(0),
                instanceOf(ClearTrashOperation.class));
    }

    @Test
    public void shouldIncAttemptsField() throws Exception {
        ClearTrashOperation operation = getClearTrashOperation();
        database.add(operation, OperationLists.State.SENT);

        database.incAttempts(database.getOperations(OperationLists.State.SENT));
        database.incAttempts(database.getOperations(OperationLists.State.SENT));
        database.incAttempts(database.getOperations(OperationLists.State.SENT));

        Operation updated = database.getOperations(OperationLists.State.SENT).get(0);
        assertThat(updated.getAttempts(), equalTo(3));
    }

    private ClearTrashOperation getClearTrashOperation() {
        return new ClearTrashOperation(null);
    }

    @Test
    public void shouldIncAttemptsOfAllGivenOperations() throws Exception {
        database.add(getClearTrashOperation(), OperationLists.State.IN_QUEUE);
        database.add(getClearTrashOperation(), OperationLists.State.SENT);
        database.add(getClearTrashOperation(), OperationLists.State.SENT);
        database.add(getClearTrashOperation(), OperationLists.State.SENT);

        database.incAttempts(database.getOperations(OperationLists.State.SENT));

        LinkedList<Operation> updated = database.getOperations(OperationLists.State.SENT);
        assertThat(updated.get(0).getAttempts(), equalTo(1));
        assertThat(updated.get(1).getAttempts(), equalTo(1));
        assertThat(updated.get(2).getAttempts(), equalTo(1));
    }

}
