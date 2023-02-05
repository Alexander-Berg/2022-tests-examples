package ru.yandex.disk.provider;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.util.Cursors;
import ru.yandex.util.Path;

import java.util.Iterator;

import static org.hamcrest.Matchers.equalTo;

public class BetterCursorWrapperTest extends DiskDatabaseMethodTest {

    @Test
    public void testShouldBeApplicableToForeachConstruction() throws Exception {
        insertRows(100);
        selection = diskDb.queryAll();
        int i = 0;
        for (DiskItem file : selection) {
            assertThat(file.getDisplayName(), equalTo("" + i));
            i++;
        }
    }

    @Test
    public void testShouldStayOnPositionAfterSecondHasNext() throws Exception {
        insertRows(5);
        selection = diskDb.queryAll();

        Iterator<DiskItem> iterator = selection.iterator();

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());

        assertThat(iterator.next().getDisplayName(), equalTo("0"));
    }

    @Test
    public void testHasNextShouldReturnFalseIfEmptyCursor() throws Exception {
        selection = diskDb.queryAll();
        Iterator<DiskItem> iterator = selection.iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testCursorAfterClearDb() throws Exception {
        insertRows(5);
        selection = diskDb.queryAll();
        diskDb.clear();
        assertFalse(selection.isClosed());
        Cursors.moveWindow(selection, 0);
        assertEquals(0, selection.getPosition());
        selection.close();
        Cursors.moveWindow(selection, 0);
        assertEquals(0, selection.getPosition());
    }

    private void insertRows(int count) {
        diskDb.beginTransaction();
        DiskItemRow row = new DiskItemRow();
        for (int i = 0; i < count; i++) {
            row.setPath(new Path(DiskDatabase.ROOT_PATH, "" + i).getPath());
            row.setDisplayName("" + i);
            row.setIsDir(false);
            diskDb.updateOrInsert(row);
        }
        diskDb.setTransactionSuccessful();
        diskDb.endTransaction();
    }
}