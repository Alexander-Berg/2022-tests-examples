package ru.yandex.disk.provider;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.FileTree.directory;
import static ru.yandex.disk.provider.FileTree.file;

public class OfflineFoldersDbTest extends AndroidTestCase2 {

    private SeclusiveContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
    }

    @Test
    public void testQueryOnlyOfflineFolders() {
        SQLiteOpenHelper2 openHelper = TestObjectsFactory.createSqlite(context);
        DiskDatabase diskDatabase = TestObjectsFactory.createDiskDatabase(openHelper);

        FileTree tree = new FileTree();
        tree.root().content(
                directory("offlineDir").setOffline(DiskItem.OfflineMark.MARKED).content(
                        directory("dirInOfflineDir")
                                .setOffline(DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY),
                        file("regularFileInOfflineDir")
                                .setOffline(DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY)
                ),
                directory("regularDir").setOffline(DiskItem.OfflineMark.NOT_MARKED),

                file("offlineFile").setOffline(DiskItem.OfflineMark.MARKED),
                file("regularFile").setOffline(DiskItem.OfflineMark.NOT_MARKED)
        );
        tree.insertToDiskDatabase(diskDatabase);


        List<String> offlineFolders = diskDatabase.getOfflineDirs();

        List<String> expectedOfflineFolders = new ArrayList<>();
        expectedOfflineFolders.add("/disk/offlineDir");

        MatcherAssert.assertThat(offlineFolders, equalTo(expectedOfflineFolders));
    }
}
