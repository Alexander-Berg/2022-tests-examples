package ru.yandex.disk.trash;

import android.util.Pair;
import org.junit.Test;
import ru.yandex.disk.operation.Operation;
import ru.yandex.disk.operation.OperationLists;
import ru.yandex.disk.operation.OperationsDatabase;
import ru.yandex.disk.operation.OperationsFactory;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.test.TestObjectsFactory;

public class TrashDatabaseTest extends AndroidTestCase2 {

    private SeclusiveContext context;
    private TrashDatabaseOpenHelper dbHelper;
    private TrashDatabase trashDb;
    private TrashListProvider trashListProvider;
    private OperationsDatabase opsDb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        dbHelper = new TrashDatabaseOpenHelper(context, "trash.test.db", 1);
        Pair<OperationsDatabase, TrashDatabase> trashDatabases = TestObjectsFactory
            .createTrashDatabases(dbHelper, new OperationsFactory(null), new EventLogger());
        opsDb = trashDatabases.first;
        trashDb = trashDatabases.second;
        trashListProvider = new TrashListProvider(dbHelper, opsDb);
    }

    @Override
    public void tearDown() throws Exception {
        dbHelper.close();
        super.tearDown();
    }

    @Test
    public void testFileCreatedOrChanged() throws Exception {
        int total = 5;
        for (int i = 0; i < total; i++) {
            trashDb.updateOrInsert(generateTrashItem(i));
        }

        TrashItemCursor items = trashListProvider.query(null);
        TrashItemCursor all = trashDb.queryAll();

        try {
            assertEquals(items.getCount(), total);
            assertEquals(all.getCount(), total);
        } finally {
            items.close();
            all.close();
        }

        opsDb.add(generateOperation(0), OperationLists.State.IN_QUEUE);
        opsDb.add(generateOperation(1), OperationLists.State.IN_QUEUE);

        items = trashListProvider.query(null);
        all = trashDb.queryAll();

        try {
            assertEquals(items.getCount(), total - 2);
            assertEquals(all.getCount(), total);
        } finally {
            items.close();
            all.close();
        }
    }

    private TrashItemRow generateTrashItem(int id) {
        return (TrashItemRow) new TrashItemRow()
                .setPath("path" + id)
                .setDisplayName("name" + id);
    }

    private Operation generateOperation(int id) {
        return new DeleteFromTrashOperation(null, "path" + id);
    }

}
