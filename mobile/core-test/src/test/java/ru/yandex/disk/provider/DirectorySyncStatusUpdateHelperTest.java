package ru.yandex.disk.provider;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.provider.FileTree.*;

@RunWith(RobolectricTestRunner.class)
public class DirectorySyncStatusUpdateHelperTest extends AndroidTestCase2  {
    private static final String SYNCING = DiskDatabase.DirectorySyncStatus.SYNCING;
    private static final String SYNCED = DiskDatabase.DirectorySyncStatus.SYNC_FINISHED;
    private DiskDatabase database;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        database = TestObjectsFactory.createDiskDatabase(
                TestObjectsFactory.createSqlite(getMockContext()));
    }

    @Test
    public void testShouldBeSyncingIfHasFileWithNullEtagLocal() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        file("a").setEtag("ETAG").setEtagLocal(null)
                )
        );

        tree.insertToDiskDatabase(database);

        database.updateSyncStatus(new Path("/disk/A"));

        DiskFileCursor all = queryAll();
        DiskItem dirA = all.get(0);
        Assert.assertEquals("/disk/A", dirA.getPath());
        assertDirectoryIsSyncing(dirA);
        all.close();
    }

    @Test
    public void testShouldBeSyncingIfHasSyncingDirectory() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        directory("B").setSyncStatus(SYNCING)
                )
        );

        tree.insertToDiskDatabase(database);

        database.updateSyncStatus(new Path("/disk/A"));

        DiskFileCursor all = queryAll();
        DiskItem dirA = all.get(0);
        Assert.assertEquals("/disk/A", dirA.getPath());
        assertDirectoryIsSyncing(dirA);
        all.close();
    }

    @Test
    public void testShouldUpdateOnlyParentDirectory() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        directory("B").content(
                                file("b").setEtag("ETAG").setEtagLocal("ETAG")
                        ),
                        file("a").setEtag("ETAG").setEtagLocal(null)
                ),
                directory("C")
        );

        tree.insertToDiskDatabase(database);

        database.updateSyncStatus(new Path("/disk/A"));

        DiskFileCursor all = queryAll();

        DiskItem dirAB = all.get(1);
        Assert.assertEquals("/disk/A/B", dirAB.getPath());
        assertDirectoryIsSynced(dirAB);

        DiskItem fileABb = all.get(2);
        Assert.assertEquals("/disk/A/B/b", fileABb.getPath());
        Assert.assertEquals(fileABb.getETag(), fileABb.getETagLocal());

        DiskItem diskC = all.get(4);
        Assert.assertEquals("/disk/C", diskC.getPath());
        assertDirectoryIsSynced(diskC);

        all.close();
    }

    @Test
    public void testShouldUpdateParentTillRootDirectory() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        directory("B").content(
                                file("b").setEtag("ETAG").setEtagLocal(null)
                        )
                )
        );

        tree.insertToDiskDatabase(database);

        database.updateSyncStatus(new Path("/disk/A/B"));

        DiskFileCursor all = queryAll();

        DiskItem dirA = all.get(0);
        Assert.assertEquals("/disk/A", dirA.getPath());
        assertDirectoryIsSyncing(dirA);

        all.close();
    }

    @Test
    public void testShouldNotUpdateParentIfSyncAlreadySyncing() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        directory("B").content(
                                file("a").setEtag("ETAG").setEtagLocal(null)
                        )
                )
        );

        tree.insertToDiskDatabase(database);
        database.updateSyncStatus(new Path("/disk/A/B"));

        database = spy(database);
        DiskDatabase.DirectorySyncStatusUpdateHelper helper = new DiskDatabase.DirectorySyncStatusUpdateHelper(database);
        helper.updateSyncStatus(new Path("/disk/A/B"));

        DiskFileCursor all = queryAll();

        DiskItem dirA = all.get(0);
        Assert.assertEquals("/disk/A", dirA.getPath());
        assertDirectoryIsSyncing(dirA);
        all.close();

        verify(database, never()).patchSyncStatus(eq(new Path("/disk/A")), nullable(String.class));
    }

    @Test
    public void testShouldNotUpdateParentIfSyncAlreadySynced() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        directory("B").content(
                                file("a").setEtag("ETAG").setEtagLocal("ETAG")
                        )
                )
        );

        tree.insertToDiskDatabase(database);
        database.updateSyncStatus(new Path("/disk/A/B"));

        database = spy(database);
        DiskDatabase.DirectorySyncStatusUpdateHelper helper = new DiskDatabase.DirectorySyncStatusUpdateHelper(database);
        helper.updateSyncStatus(new Path("/disk/A/B"));

        DiskFileCursor all = queryAll();

        DiskItem dirA = all.get(0);
        Assert.assertEquals("/disk/A", dirA.getPath());
        assertDirectoryIsSynced(dirA);
        all.close();

        verify(database, never()).patchSyncStatus(eq(new Path("/disk/A")), nullable(String.class));
    }


    @Test
    public void testShouldBeSyncedIfAllFilesSynced() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        file("a").setEtag("ETAG").setEtagLocal("ETAG")
                )
        );

        tree.insertToDiskDatabase(database);

        DiskFileCursor all = queryAll();
        DiskItem dirA = all.get(0);
        assertDirectoryIsSynced(dirA);
        all.close();
    }

    @Test
    public void testShouldBeSyncingIfHasSyncingFile() throws Exception {
        FileTree tree = new FileTree();
        tree.root().content(
                directory("A").content(
                        file("a").setEtag("ETAG").setEtagLocal("NEW_ETAG")
                )
        );

        tree.insertToDiskDatabase(database);

        database.updateSyncStatus(new Path("/disk/A"));

        DiskFileCursor all = queryAll();
        DiskItem dirA = all.get(0);
        assertDirectoryIsSyncing(dirA);
        all.close();
    }

    private DiskFileCursor queryAll() {
        return database.queryAll();
    }

    private static void assertDirectoryIsSyncing(DiskItem dir) {
        Assert.assertEquals(SYNCING, dir.getETagLocal());
    }

    private static void assertDirectoryIsSynced(DiskItem dir) {
        Assert.assertEquals(SYNCED, dir.getETagLocal());
    }

}
