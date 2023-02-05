package ru.yandex.disk.fetchfilelist;

import com.google.common.base.Function;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskFileCursor;
import ru.yandex.disk.provider.DiskItemRow;
import ru.yandex.disk.provider.FakeContentChangeNotifier;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.stats.EventLog;
import ru.yandex.disk.sync.RemoteFileItem;
import ru.yandex.disk.sync.SyncListener;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.Reflector;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.TestObjectsFactory;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public abstract class SyncListenerTest extends AndroidTestCase2 {

    protected SeclusiveContext context;
    protected DiskDatabase diskDatabase;

    private SQLiteOpenHelper2 dbOpener;
    protected DiskDatabaseSyncer syncer;

    private static final Function<RemoteFileItem, DiskItem> convertToFileItem = new Function<RemoteFileItem, DiskItem>() {

        @Override
        public DiskItem apply(RemoteFileItem file) {
            return new DiskItemBuilder()
                    .setPath(file.getPath())
                    .setIsDir(file.isDir())
                    .setSize(file.getSize())
                    .setEtag(file.getTag())
                    .build();
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        dbOpener = TestObjectsFactory.createSqlite(context);
        diskDatabase = spy(TestObjectsFactory.createDiskDatabase(dbOpener));
        syncer = createSyncer();
    }

    protected DiskDatabaseSyncer createSyncer() {
        return new DiskDatabaseSyncer<DiskItem>(diskDatabase) {
            @Override
            protected DiskFileCursor getItemsBeforeRefreshAsCursor() {
                return database.queryAll();
            }
        };
    }

    public void emulateSync(RemoteFileItem... files) throws SyncException {
        emulateSync(transform(asList(files), convertToFileItem).toArray(new DiskItem[files.length]));
    }

    public void emulateSyncEmptyList() throws SyncException {
        emulateSync(new DiskItem[] {});
    }

    public void emulateSync(DiskItem... files) throws SyncException {
        syncer.begin();
        for (DiskItem file : files) {
            syncer.collect(file);
        }
        syncer.commit();
        syncer.finish();
    }

    public void emulateSyncInterrupted(DiskItem... files) throws SyncException {
        syncer.begin();
        for (DiskItem file : files) {
            syncer.collect(file);
        }
        //skip syncer.commit();
        syncer.finish();
    }

    public void addToDb(DiskItem file) {
        DiskItemRow row = new DiskItemRow();
        row.setPath(file.getPath());
        row.setIsDir(file.isDir());
        row.setEtag(file.getETag());
        diskDatabase.updateOrInsert(row);
    }

    public void addToDb(DbFileItem file) {
        DiskItemRow row = new DiskItemRow();
        row.setPath(file.getPath());
        row.setIsDir(file.isDir());
        row.setEtag(file.getTag());
        row.setEtagLocal(file.getETagLocal());
        row.setOfflineMark(file.getOffline());
        diskDatabase.updateOrInsert(row);
    }

    @Override
    protected void tearDown() throws Exception {
        dbOpener.close();
        super.tearDown();
        Reflector.scrub(this);
    }

    protected void addSycListener(SyncListener listener) {
        syncer.addSyncListener(listener);
    }
}
