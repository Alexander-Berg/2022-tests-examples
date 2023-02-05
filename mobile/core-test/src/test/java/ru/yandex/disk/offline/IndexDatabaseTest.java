package ru.yandex.disk.offline;

import org.junit.Test;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.sql.SQLiteDatabase2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.util.Path;

import java.util.ArrayList;

import static org.hamcrest.Matchers.equalTo;

public class IndexDatabaseTest extends AndroidTestCase2 {

    private SeclusiveContext context;
    private IndexDatabaseOpenHelper dbHelper;
    private IndexDatabase indexDB;

    private IndexDatabase.Cursor queryIndexItemByPath(SQLiteDatabase2 sqldb, String resourceName) {
        Path path = new Path(resourceName);

        return new IndexDatabase.Cursor(sqldb.query("SELECT * FROM " + IndexDatabase.TABLE
                + " WHERE " + IndexDatabase.PARENT + " = ?"
                + " AND " + IndexDatabase.NAME + " = ?"
                , new String[]{path.getParentPath(), path.getName()}));
    }

    private IndexDatabase.Cursor queryAllSubitems(SQLiteDatabase2 sqldb, String resourceName) {
        return new IndexDatabase.Cursor(sqldb.query("SELECT * FROM " + IndexDatabase.TABLE
                + " WHERE " + IndexDatabase.PARENT + "||'/' LIKE ?||'/%' "
                , new String[]{resourceName}));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        dbHelper = new IndexDatabaseOpenHelper(context, "index.test.db", 1);
        indexDB = new IndexDatabase(dbHelper);
    }

    @Override
    public void tearDown() throws Exception {
        dbHelper.close();
        super.tearDown();
    }

    @Test
    public void testFileCreatedOrChanged() throws Exception {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";
        indexDB.patchFileCreatedOrChanged("mpfsid", "md5", "sha256", 100500, path, true, true, true, true);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryIndexItemByPath(sqlDb, path);

        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();

            assertEquals("mpfsid", cursor.getMpfsId());
            assertEquals(path, cursor.getPath());
            assertEquals("md5", cursor.getMd5());
            assertEquals("sha256", cursor.getSha256());
            assertEquals(100500, cursor.getFileSize());
            assertTrue(cursor.isPublic());
            assertTrue(cursor.isInvisible());
            assertTrue(cursor.isContentNotAvailable());
            assertFalse(cursor.isSharedDir());
            assertTrue(cursor.hasMetadata());
            assertFalse(cursor.isDir());

        } finally {
            cursor.close();
        }
    }

    @Test
    public void testFileCreatedOrChangedWhenAlreadyExists() throws Exception {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";

        indexDB.patchFileCreatedOrChanged("mpfsidBefore", "md5Before", "sha256Before", 100500, path, true, true, true, true);
        indexDB.patchFileCreatedOrChanged("mpfsid", "md5", "sha256", 200500, path, false, false, false, false);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryIndexItemByPath(sqlDb, path);

        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();

        assertEquals("mpfsid", cursor.getMpfsId());
        assertEquals(path, cursor.getPath());
        assertEquals("md5", cursor.getMd5());
        assertEquals("sha256", cursor.getSha256());
        assertEquals(200500, cursor.getFileSize());
        assertFalse(cursor.isPublic());
        assertFalse(cursor.isInvisible());
        assertFalse(cursor.isContentNotAvailable());
        assertFalse(cursor.isSharedDir());
        assertFalse(cursor.hasMetadata());
        assertFalse(cursor.isDir());

        cursor.close();
    }

    @Test
    public void testDirCreatedOrChanged() throws Exception {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";
        indexDB.patchDirCreatedOrChanged("mpfsid", path, true, true, true, true, true, true);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryIndexItemByPath(sqlDb, path);

        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();

        assertEquals("mpfsid", cursor.getMpfsId());
        assertEquals(path, cursor.getPath());
        assertEquals(null, cursor.getMd5());
        assertEquals(null, cursor.getSha256());
        assertEquals(0, cursor.getFileSize());
        assertTrue(cursor.isPublic());
        assertTrue(cursor.isInvisible());
        assertTrue(cursor.isContentNotAvailable());
        assertTrue(cursor.isSharedDir());
        assertTrue(cursor.hasMetadata());
        assertTrue(cursor.isDir());
        assertTrue(cursor.isReadOnly());

        cursor.close();
    }

    @Test
    public void testDirCreatedOrChangedWhenAlreadyExists() throws Exception {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";
        indexDB.patchDirCreatedOrChanged("mpfsidbefore", path, true, true, true, true, true, true);
        indexDB.patchDirCreatedOrChanged("mpfsid", path, false, false, false, false, false, false);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryIndexItemByPath(sqlDb, path);

        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();

            assertEquals("mpfsid", cursor.getMpfsId());
            assertEquals(path, cursor.getPath());
            assertEquals(null, cursor.getMd5());
            assertEquals(null, cursor.getSha256());
            assertEquals(0, cursor.getFileSize());
            assertFalse(cursor.isPublic());
            assertFalse(cursor.isInvisible());
            assertFalse(cursor.isContentNotAvailable());
            assertFalse(cursor.isSharedDir());
            assertFalse(cursor.hasMetadata());
            assertTrue(cursor.isDir());
            assertFalse(cursor.isReadOnly());

        } finally {
            cursor.close();
        }
    }

    @Test
    public void testFileDeleted() throws Exception {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";
        indexDB.patchFileCreatedOrChanged("mpfsid", "md5", "sha256", 100500, path, true, true, true, true);

        indexDB.patchDeleted(path);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryIndexItemByPath(sqlDb, path);
        int count = cursor.getCount();
        cursor.close();

        assertEquals(0, count);
    }

    @Test
    public void testDirDeleted() throws Exception {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";
        indexDB.patchDirCreatedOrChanged("mpfsid", path, true, true, true, true, true, true);

        indexDB.patchDeleted(path);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryIndexItemByPath(sqlDb, path);
        int count = cursor.getCount();
        cursor.close();

        assertEquals(0, count);
    }

    @Test
    public void testDeletingDirShouldDeleteSubitems() {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/resourcename";
        indexDB.patchDirCreatedOrChanged("mpfsid", path, true, true, true, true, true, true);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, path + "/subfile", true, true, true, true);
        indexDB.patchDirCreatedOrChanged(null, path + "/subdir", true, true, true, true, true, true);
        indexDB.patchDeleted(path);

        SQLiteDatabase2 sqlDb = dbHelper.getReadableDatabase();
        IndexDatabase.Cursor cursor = queryAllSubitems(sqlDb, path);
        int count = cursor.getCount();
        cursor.close();

        assertEquals(0, count);
    }

    @Test
    public void testDeletingByPathShouldDeleteParentDir() {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/dir";
        indexDB.patchDirCreatedOrChanged(null, path, false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, path + "/subDir", false, false, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, path + "/subDir/subFile.txt", false, false, false, false);
        indexDB.deleteByPath(path);

        IndexDatabase.Cursor cursor = indexDB.queryRecursively(path);
        int count = cursor.getCount();
        cursor.close();

        assertEquals(0, count);
    }

    @Test
    public void testTrickyDeletingByPath() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        indexDB.patchDirCreatedOrChanged(null, root + "/A", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/AB", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/AB/C", false, false, false, false, false, false);

        indexDB.deleteByPath(root + "/A_");

        IndexDatabase.Cursor cursor = indexDB.queryRecursively(root + "/AB/C");
        int count = cursor.getCount();
        cursor.close();

        assertEquals(1, count);
    }

    @Test
    public void testTrickyDeletingByPathCaseSensitiveLike() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        indexDB.patchFileCreatedOrChanged("mpfsid", null, null, 0, root + "/file.txt", false, false, false, false);
        indexDB.patchFileCreatedOrChanged("mpfsid", null, null, 0, root + "/filE.txt", false, false, false, false);

        indexDB.deleteByPath(root + "/file.txt");

        IndexDatabase.Cursor cursor = indexDB.queryRecursively(root);
        int count = cursor.getCount();
        cursor.close();

        assertEquals(1, count);
    }

    @Test
    public void testQueryItemsInDir() {
        String root = DiskDatabase.ROOT_PATH.getPath();

        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/dirToList/file.txt", false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/dirToList/deeper/deeperFile.txt", false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/upperFile.txt", false, false, false, false);

        ArrayList<String> expectedFiles = new ArrayList<>();
        expectedFiles.add(root + "/dirToList/file.txt");

        ArrayList<String> actualFiles = new ArrayList<>();
        IndexDatabase.Cursor cursor = indexDB.queryItemsInDir(root + "/dirToList");
        try {
            while (cursor.moveToNext()) {
                actualFiles.add(cursor.getPath());
            }
        } finally {
            cursor.close();
        }

        assertThat(actualFiles, equalTo(expectedFiles));
    }

    @Test
    public void testQueryItemsInDirTrickyPercentCase() {
        String root = DiskDatabase.ROOT_PATH.getPath();

        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/%B/good.txt", false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/AB/evil.txt", false, false, false, false);

        ArrayList<String> expectedFiles = new ArrayList<>();
        expectedFiles.add(root + "/%B/good.txt");

        ArrayList<String> actualFiles = new ArrayList<>();
        IndexDatabase.Cursor cursor = indexDB.queryItemsInDir(root + "/%B");
        try {
            while (cursor.moveToNext()) {
                actualFiles.add(cursor.getPath());
            }
        } finally {
            cursor.close();
        }

        assertThat(actualFiles, equalTo(expectedFiles));
    }

    @Test
    public void testQueryRecursively() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        indexDB.patchDirCreatedOrChanged(null, root + "/dir", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/dir/subdir", false, false, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/dir/subfile", false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/dirsubdir", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/otherdir", false, false, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/otherfile", false, false, false, false);

        ArrayList<String> expectedItems = new ArrayList<>();
        expectedItems.add(root + "/dir");
        expectedItems.add(root + "/dir/subfile");
        expectedItems.add(root + "/dir/subdir");

        ArrayList<String> actualItems = new ArrayList<>();
        IndexDatabase.Cursor cursor = indexDB.queryRecursively(root + "/dir");
        try {
            while (cursor.moveToNext()) {
                actualItems.add(cursor.getPath());
            }
        } finally {
            cursor.close();
        }

        assertThat(actualItems, equalTo(expectedItems));
    }

    @Test
    public void testQueryRecursivelyOrder() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        indexDB.patchDirCreatedOrChanged(null, root + "/A", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/A/A", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/A/A/A", false, false, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/A/A/A/a", false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/A/A/a", false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/A/B", false, false, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/A/a", false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/A/b", false, false, false, false);
        ArrayList<String> expectedItems = new ArrayList<>();
        expectedItems.add(root + "/A");
        expectedItems.add(root + "/A/a");
        expectedItems.add(root + "/A/b");
        expectedItems.add(root + "/A/A");
        expectedItems.add(root + "/A/B");
        expectedItems.add(root + "/A/A/a");
        expectedItems.add(root + "/A/A/A");
        expectedItems.add(root + "/A/A/A/a");

        ArrayList<String> actualItems = new ArrayList<>();
        IndexDatabase.Cursor cursor = indexDB.queryRecursively(root + "/A");
        try {
            while (cursor.moveToNext()) {
                actualItems.add(cursor.getPath());
            }
        } finally {
            cursor.close();
        }

        assertThat(actualItems, equalTo(expectedItems));
    }

    @Test
    public void testQueryRecursivelyTrickyPercentCase() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        indexDB.patchDirCreatedOrChanged(null, root + "/%B", false, true, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/%B/good.txt", false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/AB", false, true, false, false, false, false);
        indexDB.patchFileCreatedOrChanged(null, null, null, 0, root + "/AB/evil.txt", false, false, false, false);

        ArrayList<String> expectedFiles = new ArrayList<>();
        expectedFiles.add(root + "/%B");
        expectedFiles.add(root + "/%B/good.txt");

        ArrayList<String> actualFiles = new ArrayList<>();
        IndexDatabase.Cursor cursor = indexDB.queryRecursively(root + "/%B");
        try {
            while (cursor.moveToNext()) {
                actualFiles.add(cursor.getPath());
            }
        } finally {
            cursor.close();
        }

        assertThat(actualFiles, equalTo(expectedFiles));
    }

    @Test
    public void testUpdateIndexEtag() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        indexDB.patchDirCreatedOrChanged(null, root + "/offlinedir", false, false, false, false, false, false);
        indexDB.patchDirCreatedOrChanged(null, root + "/otherdir", false, false, false, false, false, false);

        indexDB.updateIndexEtag(root + "/offlinedir", "OFFLINE_DIR_ETAG");

        IndexDatabase.Cursor dirCursor = indexDB.queryItemByPath(root + "/offlinedir");
        dirCursor.moveToFirst();
        try {
            String indexEtag = dirCursor.getIndexEtag();
            assertEquals("OFFLINE_DIR_ETAG", indexEtag);
        } finally {
            dirCursor.close();
        }

        IndexDatabase.Cursor otherDirCursor = indexDB.queryItemByPath(root + "/otherdir");
        otherDirCursor.moveToFirst();
        try {
            String indexEtag = otherDirCursor.getIndexEtag();
            assertNull(indexEtag);
        } finally {
            otherDirCursor.close();
        }
    }

    @Test
    public void testPatchingDirShouldNotEraseIndexEtag() {
        String path = DiskDatabase.ROOT_PATH.getPath() + "/dir";
        indexDB.patchDirCreatedOrChanged(null, path, false, false, false, false, false, false);
        indexDB.updateIndexEtag(path, "INDEX_ETAG");

        indexDB.patchDirCreatedOrChanged(null, path, true, false, false, false, false, false);

        IndexDatabase.Cursor cur = indexDB.queryItemByPath(path);
        cur.moveToFirst();
        assertEquals("INDEX_ETAG", cur.getIndexEtag());
        cur.close();
    }

    @Test
    public void testShouldInsertIfNotExists() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        IndexRow row = new IndexRow()
                .setParent(root).setName("dir").setIsDir(true);

        indexDB.updateOrInsert(row);

        IndexDatabase.Cursor cursor = indexDB.queryAll();
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(root + "/dir", cursor.getPath());
        cursor.close();
    }


    @Test
    public void testShouldUpdateIfExists() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        IndexRow dirBefore = new IndexRow()
                .setParent(root).setName("dir").setIsDir(true).setIndexEtag("INDEX_ETAG");

        indexDB.updateOrInsert(dirBefore);

        IndexRow dirAfter = new IndexRow()
                .setParent(root).setName("dir").setIsDir(true);

        indexDB.updateOrInsert(dirAfter);

        IndexDatabase.Cursor cursor = indexDB.queryAll();
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(root + "/dir", cursor.getPath());
        assertEquals("INDEX_ETAG", cursor.getIndexEtag());
        cursor.close();
    }

    @Test
    public void testDirWithEtagAfterBecamingFileShouldNotContainEtag() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        IndexRow dirBefore = new IndexRow()
                .setParent(root).setName("filedir").setIsDir(true).setIndexEtag("INDEX_ETAG");

        IndexRow fileAfter = new IndexRow()
                .setParent(root).setName("filedir").setIsDir(false);

        indexDB.updateOrInsert(dirBefore);

        indexDB.updateOrInsert(fileAfter);

        IndexDatabase.Cursor cursor = indexDB.queryAll();
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(root + "/filedir", cursor.getPath());
        assertEquals(null, cursor.getIndexEtag());
        cursor.close();
    }

    @Test
    public void testFileAfterBecamingDirShouldNotContainSha256() {
        String root = DiskDatabase.ROOT_PATH.getPath();
        IndexRow fileBefore = new IndexRow()
                .setParent(root).setName("filedir").setIsDir(false).setSha256("SHA256");

        IndexRow dirAfter = new IndexRow()
                .setParent(root).setName("filedir").setIsDir(true);

        indexDB.updateOrInsert(fileBefore);
        indexDB.updateOrInsert(dirAfter);

        IndexDatabase.Cursor cursor = indexDB.queryAll();
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals(root + "/filedir", cursor.getPath());
        assertEquals(null, cursor.getSha256());
        cursor.close();
    }
}