package ru.yandex.disk.provider;

import android.database.Cursor;
import android.widget.ListView;
import org.junit.Test;
import ru.yandex.disk.SortOrder;
import ru.yandex.util.Path;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.disk.sql.SQLVocabulary.DESC_;

public class FindPositionTest extends DiskDatabaseMethodTest {

    @Test
    public void testFindFilePosition() throws Exception {
        FileTree.create().content(directory("A")
                .content(
                        directory("B").setLastModified(10),
                        directory("AB").setLastModified(11),
                        directory("C").setLastModified(12),
                        file("aa").setEtime(5).setLastModified(1),
                        file("dd").setEtime(4).setLastModified(2),
                        file("cc").setEtime(3).setLastModified(3),
                        file("bb").setEtime(1).setLastModified(4),
                        file("ff").setEtime(2).setLastModified(5)
                )).insertToDiskDatabase(diskDb);

        // File by name
        int pos = findFilePosition(Path.asPath("/disk/A/bb"),
                DiskFileCursor.IS_DIR + DESC_ + SortOrder.NAME_ASC.getDbSort());
        assertThat(pos, equalTo(4));

        // Folder by name
        pos = findFilePosition(Path.asPath("/disk/A/B"),
                DiskFileCursor.IS_DIR + DESC_ + SortOrder.NAME_ASC.getDbSort());
        assertThat(pos, equalTo(1));

        // File by time
        pos = findFilePosition(Path.asPath("/disk/A/bb"),
                DiskFileCursor.IS_DIR + DESC_ + SortOrder.MTIME_DESC.getDbSort());
        assertThat(pos, equalTo(4));

        // Folder by time
        pos = findFilePosition(Path.asPath("/disk/A/B"),
                DiskFileCursor.IS_DIR + DESC_ + SortOrder.MTIME_DESC.getDbSort());
        assertThat(pos, equalTo(2));
    }

    private int findFilePosition(Path path, String sort) {
        ContentRequest cr = ContentRequestFactory.newDirectoryListRequest("/disk/A", null);
        final ContentRequest findPositionContentRequest =
                ContentRequestFactory.newFindFilePositionRequest(cr, sort, path, false, false);

        int position = ListView.INVALID_POSITION;
        try (final Cursor cursor = diskDb.query(findPositionContentRequest)) {
            if (cursor.moveToFirst()) {
                position = cursor.getInt(0) - 1;
            }
        }

        return position;
    }
}
