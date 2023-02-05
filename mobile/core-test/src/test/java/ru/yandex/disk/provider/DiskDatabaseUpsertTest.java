package ru.yandex.disk.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static ru.yandex.util.Path.asPath;

@RunWith(RobolectricTestRunner.class)
public class DiskDatabaseUpsertTest extends AndroidTestCase2 {

    private SeclusiveContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
    }

    @Test
    public void testDirAfterFile() throws Exception {
        DiskDatabase diskDatabase = TestObjectsFactory.createDiskDatabase(createDbOpenHelper(),
                null, null);

        String root = DiskDatabase.ROOT_PATH.getPath();
        DiskItemRow fileRow = new DiskItemRow()
                .setPath(root, "FFF")
                .setDisplayName(null)
                .setMimeType("text/plain")
                .setSize(100500)
                .setIsDir(false)
                .setEtag("file.md5")
                .setLastModified(42)
                .setShared(true)
                .setReadonly(true)
                .setPublicUrl("http://file.link")
                .setEtime(0xEEEE)
                .setMediaType("document")
                .setOfflineMark(DiskItem.OfflineMark.NOT_MARKED)
                .setEtagLocal("file.etaglocal")
                .setMpfsId("file.mpfsid")
                .setHasThumbnail(true);

        DiskItemRow dirRow = new DiskItemRow()
                .setPath(root, "FFF")
                .setMpfsId("dir.mpfsid")
                .setIsDir(true)
                .setPublicUrl(null)
                .setShared(false);

        diskDatabase.updateOrInsert(fileRow);
        diskDatabase.updateOrInsert(dirRow);


        DiskFileCursor cursor = diskDatabase.queryAll();
        cursor.moveToFirst();
        try {
            assertEquals(1, cursor.getCount());

            Path path = new Path(cursor.getPath());
            assertEquals(root, path.getParentPath());
            assertEquals("FFF", path.getName());
            assertEquals(null, cursor.getDisplayName());
            assertEquals(null, cursor.getMimeType());
            assertEquals(0, cursor.getSize());
            assertEquals(true, cursor.isDir());
            assertEquals(null, cursor.getETag());
            assertEquals(0, cursor.getLastModified());
            assertEquals(false, cursor.isShared());
            assertEquals(false, cursor.isReadonly());
            assertEquals(null, cursor.getDisplayToLowerCase());
            assertEquals(null, cursor.getPublicUrl());
            assertEquals(0, cursor.getEtime());
            assertEquals(null, cursor.getMediaType());
            assertEquals(DiskItem.OfflineMark.NOT_MARKED, cursor.getOffline());
            assertEquals(null, cursor.getETagLocal());
            assertEquals("dir.mpfsid", cursor.getMpfsFileId());
            assertEquals(false, cursor.getHasThumbnail());

        } finally {
            cursor.close();
        }
    }

    @Test
    public void testFileAfterDir() {

        DiskDatabase diskDatabase = TestObjectsFactory.createDiskDatabase(createDbOpenHelper(), null, null);

        String root = DiskDatabase.ROOT_PATH.getPath();
        DiskItemRow dirRow = new DiskItemRow()
                .setPath(root, "FFF")
                .setDisplayName(null)
                .setMimeType(null)
                .setIsDir(true)
                .setShared(true)
                .setReadonly(true)
                .setPublicUrl("http://dir.link")
                .setOfflineMark(DiskItem.OfflineMark.NOT_MARKED)
                .setEtagLocal("dir.etaglocal")
                .setMpfsId("dir.mpfsid");

        DiskItemRow fileRow = new DiskItemRow()
                .setIsDir(false)
                .setPath(root, "FFF")
                .setMpfsId("file.mpfsid")
                .setEtag("file.md5")
                .setSize(100500)
                .setPublicUrl("FAKE LINK");


        diskDatabase.updateOrInsert(dirRow);
        diskDatabase.updateOrInsert(fileRow);

        DiskFileCursor cursor = diskDatabase.queryAll();
        cursor.moveToFirst();
        try {
            assertEquals(1, cursor.getCount());

            Path path = asPath(cursor.getPath());
            assertEquals(root, path.getParentPath());
            assertEquals("FFF", path.getName());
            assertEquals(null, cursor.getDisplayName());
            assertEquals(null, cursor.getMimeType());
            assertEquals(100500, cursor.getSize());
            assertEquals(false, cursor.isDir());
            assertEquals("file.md5", cursor.getETag());
            assertEquals(0, cursor.getLastModified());
            assertEquals(false, cursor.isShared());
            assertEquals(false, cursor.isReadonly());
            assertEquals(null, cursor.getDisplayToLowerCase());
            assertEquals("FAKE LINK", cursor.getPublicUrl());
            assertEquals(0, cursor.getEtime());
            assertEquals(null, cursor.getMediaType());
            assertEquals(DiskItem.OfflineMark.NOT_MARKED, cursor.getOffline());
            assertEquals(null, cursor.getETagLocal());
            assertEquals("file.mpfsid", cursor.getMpfsFileId());
            assertEquals(false, cursor.getHasThumbnail());

        } finally {
            cursor.close();
        }
    }

    @Test
    public void testFileAfterFile() {
        DiskDatabase diskDatabase = TestObjectsFactory.createDiskDatabase(createDbOpenHelper(), null, null);

        long SIZE_BEFORE = 42;
        long SIZE_AFTER = 100500;

        long LAST_MODIFIED = 111111111;

        String root = DiskDatabase.ROOT_PATH.getPath();
        DiskItemRow fileRow = new DiskItemRow()
                .setPath(root, "file.txt")
                .setDisplayName("displayname")
                .setMimeType("text/plain")
                .setSize(SIZE_BEFORE)
                .setIsDir(false)
                .setEtag("file.md5.before")
                .setLastModified(LAST_MODIFIED)
                .setShared(false)
                .setReadonly(true)
                .setPublicUrl("http://file.link")
                .setEtime(0xEEEE)
                .setMediaType("document")
                .setOfflineMark(DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY)
                .setEtagLocal("file.etaglocal")
                .setMpfsId("file.mpfsid")
                .setHasThumbnail(true);

        DiskItemRow fileRowAfter = new DiskItemRow()
                .setIsDir(false)
                .setPath(root, "file.txt")
                .setMpfsId("file.mpfsid")
                .setEtag("file.md5.after")
                .setSize(SIZE_AFTER)
                .setPublicUrl("FAKE LINK");

        diskDatabase.updateOrInsert(fileRow);
        diskDatabase.updateOrInsert(fileRowAfter);

        DiskFileCursor cursor = diskDatabase.queryAll();
        cursor.moveToFirst();
        try {
            assertEquals(1, cursor.getCount());

            Path path = asPath(cursor.getPath());
            assertEquals(root, path.getParentPath());
            assertEquals("file.txt", path.getName());
            assertEquals("displayname", cursor.getDisplayName());
            assertEquals("text/plain", cursor.getMimeType());
            assertEquals(SIZE_AFTER, cursor.getSize());
            assertEquals(false, cursor.isDir());
            assertEquals("file.md5.after", cursor.getETag());
            assertEquals(LAST_MODIFIED, cursor.getLastModified());
            assertEquals(false, cursor.isShared());
            assertEquals(true, cursor.isReadonly());
            assertEquals("displayname", cursor.getDisplayToLowerCase());
            assertEquals("FAKE LINK", cursor.getPublicUrl());//as for now, we decided to update real link with "FAKE LINK" from index for simplicity =(
            assertEquals(0xEEEE, cursor.getEtime());
            assertEquals("document", cursor.getMediaType());
            assertEquals(DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY, cursor.getOffline());
            assertEquals("file.etaglocal", cursor.getETagLocal());
            assertEquals("file.mpfsid", cursor.getMpfsFileId());
            assertEquals(true, cursor.getHasThumbnail());

        } finally {
            cursor.close();
        }
    }

    @Test
    public void testDirAfterDir() {

        DiskDatabase diskDatabase = TestObjectsFactory.createDiskDatabase(createDbOpenHelper(), null, null);
        String root = DiskDatabase.ROOT_PATH.getPath();
        DiskItemRow dirRow = new DiskItemRow()
                .setPath(root, "dir")
                .setDisplayName("displayname")
                .setIsDir(true)
                .setShared(true)
                .setReadonly(false)
                .setPublicUrl("http://dir.link")
                .setOfflineMark(DiskItem.OfflineMark.MARKED)
                .setEtagLocal("dir.etaglocal")
                .setMpfsId("dir.mpfsid");

        DiskItemRow dirRowAfter = new DiskItemRow()
                .setPath(root, "dir")
                .setIsDir(true)
                .setShared(false)
                .setPublicUrl("FAKE LINK")
                .setReadonly(true)
                .setOfflineMark(DiskItem.OfflineMark.NOT_MARKED)
                .setMpfsId("dir.mpfsid");

        diskDatabase.updateOrInsert(dirRow);
        diskDatabase.updateOrInsert(dirRowAfter);

        DiskFileCursor cursor = diskDatabase.queryAll();
        cursor.moveToFirst();
        try {
            assertEquals(1, cursor.getCount());

            Path path = new Path(cursor.getPath());
            assertEquals(root, path.getParentPath());
            assertEquals("dir", path.getName());
            assertEquals("displayname", cursor.getDisplayName());
            assertEquals(null, cursor.getMimeType());
            assertEquals(0, cursor.getSize());
            assertEquals(true, cursor.isDir());
            assertEquals(null, cursor.getETag());
            assertEquals(0, cursor.getLastModified());
            assertEquals(false, cursor.isShared());
            assertEquals(true, cursor.isReadonly());
            assertEquals("displayname", cursor.getDisplayToLowerCase());
            assertEquals("FAKE LINK", cursor.getPublicUrl());//as for now, we decided to update real link with "FAKE LINK" from index for simplicity =(
            assertEquals(0, cursor.getEtime());
            assertEquals(null, cursor.getMediaType());
            assertEquals(DiskItem.OfflineMark.NOT_MARKED, cursor.getOffline());
            assertEquals("dir.etaglocal", cursor.getETagLocal());
            assertEquals("dir.mpfsid", cursor.getMpfsFileId());
            assertEquals(false, cursor.getHasThumbnail());

        } finally {
            cursor.close();
        }
    }

    private SQLiteOpenHelper2 createDbOpenHelper() {
        return TestObjectsFactory.createSqlite(context);
    }

}
