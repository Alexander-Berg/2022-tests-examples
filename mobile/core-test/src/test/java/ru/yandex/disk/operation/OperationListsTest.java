package ru.yandex.disk.operation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.trash.ClearTrashOperation;
import ru.yandex.disk.trash.DeleteFromTrashOperation;
import ru.yandex.disk.trash.TrashDatabaseOpenHelper;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(RobolectricTestRunner.class)
public class OperationListsTest {

    private OperationLists.State state = OperationLists.State.IN_QUEUE;
    private OperationLists impl;

    @Before
    public void setUp() throws Exception {
        impl = createOperationLists();
    }

    private OperationLists createOperationLists() {
        return TestObjectsFactory.createOperationsDatabase(
            new TrashDatabaseOpenHelper(RuntimeEnvironment.application), new OperationsFactory(null));
    }

    @Test
    public void shouldRemove() throws Exception {
        Operation operation = getClearTrashOperation();
        impl.add(operation, state);

        impl.remove(operation);

        List<Operation> operations = impl.getOperations(state);
        assertThat(operations.size(), equalTo(0));
        assertThat(impl.getOperations(state).size() == 0, equalTo(true));
    }

    private ClearTrashOperation getClearTrashOperation() {
        return new ClearTrashOperation(null);
    }

    @Test
    public void shouldRestoreCorrectClass() throws Exception {
        impl.add(new ClearTrashOperation(null), state);
        impl.add(new DeleteFromTrashOperation(null, "/some/path"), state);

        Operation operation = impl.getOperations(state).get(0);
        assertThat(operation, instanceOf(ClearTrashOperation.class));

        impl.remove(operation);

        assertThat(impl.getOperations(state).get(0), instanceOf(DeleteFromTrashOperation.class));
    }

    @Test
    public void shouldRemoveById() throws Exception {
        Operation first = getClearTrashOperation();
        impl.add(first, state);
        Operation second = getClearTrashOperation();
        impl.add(second, state);

        impl.remove(first);

        assertThat(impl.getOperations(state).get(0), equalTo(second));
    }

    @Test
    public void shouldPeekFistAddedAfterRequest() throws Exception {
        Operation first = getClearTrashOperation();
        impl.add(first, state);
        Operation second = getClearTrashOperation();
        impl.add(second, state);

        impl.update(first, state);

        assertThat(impl.getOperations(state).get(0), equalTo(second));
    }

    @Test
    public void shouldRestoreMaxAdded() throws Exception {
        ClearTrashOperation first = getClearTrashOperation();
        impl.add(first, state);
        Operation second = getClearTrashOperation();
        impl.add(second, state);
        impl.remove(first);

        impl = createOperationLists();
        impl.add(getClearTrashOperation(), state);

        assertThat(impl.getOperations(state).get(0), equalTo(second));
    }
}
