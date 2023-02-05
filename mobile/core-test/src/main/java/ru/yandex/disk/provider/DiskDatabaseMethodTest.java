package ru.yandex.disk.provider;

import ru.yandex.disk.DiskItem;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import java.util.ArrayList;
import java.util.List;

public abstract class DiskDatabaseMethodTest extends AndroidTestCase2 {

    protected DiskDatabase diskDb;

    private DH dbOpener;

    protected DiskFileCursor selection;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dbOpener = new DH(mContext);
        diskDb = TestObjectsFactory.createDiskDatabase(dbOpener, new ContentChangeNotifier(null) {
            @Override
            public void notifyChange(Path path) {
                throw new UnsupportedOperationException
                ("DiskDatabase method have not notify about changes. Client should do that.");

            }
        }, null);
    }

    @Override
    public void tearDown() throws Exception {
        if (selection != null) {
            selection.close();
            selection = null;
        }
        dbOpener.close();
        super.tearDown();
    }

    public static List<String> asPathsList(DiskFileCursor selection) {
        ArrayList<String> paths = new ArrayList<>();
        for (DiskItem file : selection) {
            paths.add(file.getPath());
        }
        return paths;
    }

}
