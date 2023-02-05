package ru.yandex.disk.offline;

import org.junit.Test;
import ru.yandex.disk.client.IndexItem;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;

public class StoringToDbIndexHandlerTest extends AndroidTestCase2 {

    private SeclusiveContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
    }

    private byte[] byteArr(int... bytes) {
        byte[] res = new byte[bytes.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte) bytes[i];
        }
        return res;
    }

    @Test
    public void testFileCreatedOrChanged() throws Exception {

        byte[] MD5 = byteArr(0xCA, 0xFE, 0xBA, 0xBE);
        byte[] SHA256 = byteArr(0x00, 0xC0, 0xFF, 0xEE);

        IndexDatabase indexDb = new IndexDatabase(new IndexDatabaseOpenHelper(context, "index.test.db", 1));


        StoringToDbIndexHandler handler = new StoringToDbIndexHandler(indexDb);

        IndexItem item = new IndexItem.Builder()
                .setOp(IndexItem.Operation.file_created_or_changed)
                .setFullPath("fullpath")
                .setContentLength(5984726734L)
                .setMd5(MD5)
                .setSha256(SHA256)
                .build();

        handler.handleItem(item);

        IndexDatabase.Cursor cursor = indexDb.queryItemByPath("/disk/" + item.getFullPath());
        cursor.moveToFirst();
        try {
            assertEquals("/disk/" + item.getFullPath(), cursor.getPath());
            assertEquals(5984726734L, cursor.getFileSize());
            assertEquals(false, cursor.isDir());
            assertEquals("cafebabe", cursor.getMd5());
            assertEquals("00c0ffee", cursor.getSha256());
        } finally {
            cursor.close();
        }

    }

    @Test
    public void testDirCreatedOrChanged() throws Exception {
        IndexDatabase indexDb = new IndexDatabase(new IndexDatabaseOpenHelper(context, "index.test.db", 1));

        StoringToDbIndexHandler handler = new StoringToDbIndexHandler(indexDb);

        IndexItem item = new IndexItem.Builder()
                .setOp(IndexItem.Operation.dir_created_or_changed)
                .setFullPath("fullPathToDir")
                .build();

        handler.handleItem(item);


        IndexDatabase.Cursor cursor = indexDb.queryItemByPath("/disk/" + item.getFullPath());
        cursor.moveToFirst();
        try {
            assertEquals("/disk/" + item.getFullPath(), cursor.getPath());
            assertEquals(true, cursor.isDir());
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testDeleted() throws Exception {

        IndexDatabase indexDb = new IndexDatabase(new IndexDatabaseOpenHelper(context, "index.test.db", 1));
        indexDb.patchFileCreatedOrChanged("mpfsid", "md5", "sha256", 100500L, "fullPathToFile", false, false, false, false);

        StoringToDbIndexHandler handler = new StoringToDbIndexHandler(indexDb);

        IndexItem item = new IndexItem.Builder()
                .setOp(IndexItem.Operation.deleted)
                .setFullPath("fullPathToFile")
                .build();

        handler.handleItem(item);

        IndexDatabase.Cursor cursor = indexDb.queryItemByPath("/disk/" + item.getFullPath());
        try {
            assertEquals(0, cursor.getCount());
        } finally {
            cursor.close();
        }

    }

}
