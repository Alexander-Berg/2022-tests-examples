package ru.yandex.disk.test;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Pair;
import androidx.annotation.NonNull;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.Mocks;
import ru.yandex.disk.Storage;
import ru.yandex.disk.app.DiskServicesAnalyzer;
import ru.yandex.disk.app.DiskServicesScanner;
import ru.yandex.disk.asyncbitmap.LegacyPreviewsDatabase;
import ru.yandex.disk.asyncbitmap.PreviewsDatabaseCreator;
import ru.yandex.disk.autoupload.AutouploadCheckDebouncer;
import ru.yandex.disk.autoupload.observer.StorageListProvider;
import ru.yandex.disk.background.BackgroundWatcher;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.download.DownloadQueueDH;
import ru.yandex.disk.download.DownloadQueueDatabase;
import ru.yandex.disk.event.EventSender;
import ru.yandex.disk.feed.FeedBlock;
import ru.yandex.disk.feed.FeedDatabase;
import ru.yandex.disk.feed.FeedDatabaseSchemaCreator;
import ru.yandex.disk.feed.ImmutableContentBlock;
import ru.yandex.disk.fetchfilelist.PathLock;
import ru.yandex.disk.multilogin.QueueSqliteHelperProvider;
import ru.yandex.disk.operation.OperationsDatabase;
import ru.yandex.disk.operation.OperationsFactory;
import ru.yandex.disk.operation.OperationsSchemeCreator;
import ru.yandex.disk.photoslice.MomentsDatabase;
import ru.yandex.disk.photoslice.MomentsDatabaseSchemeCreator;
import ru.yandex.disk.photoslice.TableSyncSuffixesCreator;
import ru.yandex.disk.provider.ContentChangeNotifier;
import ru.yandex.disk.provider.DH;
import ru.yandex.disk.provider.DiskContentProvider;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskTableSchemeCreator;
import ru.yandex.disk.provider.DiskUriProcessorMatcher;
import ru.yandex.disk.provider.DiskViewsCreator;
import ru.yandex.disk.provider.IncidentContentResolver;
import ru.yandex.disk.provider.Settings;
import ru.yandex.disk.provider.SettingsDH;
import ru.yandex.disk.provider.UploadQueueTableCreator;
import ru.yandex.disk.provider.offline.PendingOperationsTableCreator;
import ru.yandex.disk.replication.NeighborsContentProviderClient;
import ru.yandex.disk.replication.SelfContentProviderClient;
import ru.yandex.disk.replication.SettingsReplicator;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.strictmode.ReleaseStrictModeManager;
import ru.yandex.disk.toggle.NewOnboardingsToggle;
import ru.yandex.disk.trash.TrashDatabase;
import ru.yandex.disk.trash.TrashDatabaseOpenHelper;
import ru.yandex.disk.trash.TrashDatabaseSchemeCreator;
import ru.yandex.disk.trash.TrashListProvider;
import ru.yandex.disk.upload.DiskQueueSerializer;
import ru.yandex.disk.upload.FilesTimeExtractor;
import ru.yandex.disk.upload.UploadQueue;
import ru.yandex.disk.upload.UploadQueueSerializer;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.util.SystemClock;

import javax.annotation.NonnullByDefault;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.disk.provider.DiskContract.Queue.MEDIA_TYPE_CODE;

@NonnullByDefault
public class TestObjectsFactory {
    public static final String DB_NAME = "test";

    public static DiskDatabase createDiskDatabase(final SQLiteOpenHelper2 sqliteHelper) {
        return createDiskDatabase(sqliteHelper, mock(ContentChangeNotifier.class), new PathLock());
    }

    public static FeedDatabase createFeedDatabase(
        final SQLiteOpenHelper2 sqLiteHelper, final Settings settings,
        final DiskDatabase diskDatabase
    ) {
        final FeedDatabase feedDatabase = new FeedDatabase(sqLiteHelper, settings, diskDatabase);
        sqLiteHelper.addDatabaseOpenListener(new FeedDatabaseSchemaCreator());
        return feedDatabase;
    }

    public static Settings createSettings(final Context context) {
        return createSettings(context, null);
    }

    public static Settings createSettings(final Context context,
                                          @Nullable final SQLiteOpenHelper2 sqliteHelper) {
        return new Settings(createSettingsReplicator(context, sqliteHelper), context.getContentResolver(),
                context.getPackageName(), null, new SettingsDH(context), createStrictModeManager(),
                Collections.emptyList());
    }

    public static ReleaseStrictModeManager createStrictModeManager() {
        return new ReleaseStrictModeManager(mock(Diagnostics.class));
    }

    public static ApplicationSettings createApplicationSettings(final Context context) {
        return createApplicationSettings(context, SystemClock.REAL);
    }

    public static ApplicationSettings createApplicationSettings(final Context context, final SystemClock systemClock) {
        return new ApplicationSettings(
            TestObjectsFactory.createSettings(context),
            mock(EventSender.class),
            systemClock,
            mock(BackgroundWatcher.class),
            Collections::emptyList
        );
    }

    public static SelfContentProviderClient createSelfContentProviderClient(final Context context) {
        return createSelfContentProviderClient(context, null);
    }

    public static SelfContentProviderClient createSelfContentProviderClient(
            final Context context,
            @Nullable final SQLiteOpenHelper2 db) {
        final IncidentContentResolver contentResolver =
                new IncidentContentResolver(context.getContentResolver());
        final DiskUriProcessorMatcher diskUriProcessorMatcher =
                createDiskUriProcessorMatcher(context, db);
        return new SelfContentProviderClient(
                DiskContentProvider.getAuthority(context), contentResolver,
                () -> diskUriProcessorMatcher);
    }

    public static SettingsReplicator createSettingsReplicator(final Context context,
                                                              @Nullable final SQLiteOpenHelper2 sqliteHelper) {
        final SelfContentProviderClient selfContentProviderClient =
            createSelfContentProviderClient(context, sqliteHelper);
        final NeighborsContentProviderClient neighborsProviderClient =
                createNeighborsProviderClient(context);

        return new SettingsReplicator(neighborsProviderClient, selfContentProviderClient,
            Collections.emptyList());
    }

    public static SQLiteOpenHelper2 createSqlite(final Context context) {
        return createSqlite(context, DB_NAME, 1);
    }

    private static SQLiteOpenHelper2 createSqlite(final Context context,
                                                  final String dbName, final int version) {
        return new TestSQLiteOpenHelper2(context, dbName, version);
    }

    public static NeighborsContentProviderClient createNeighborsProviderClient(final Context context) {
        final String authority = DiskContentProvider.getAuthority(context);
        final IncidentContentResolver cr = new IncidentContentResolver(context.getContentResolver());

        final PackageManager pm = context.getPackageManager();

        final SeclusiveContext seclusiveContext = new SeclusiveContext(context);
        CredentialsManager credentialsManager = Mocks.initCredentials(seclusiveContext);
        final DiskServicesScanner diskServicesScanner = new DiskServicesScanner(cr, pm, context,
                mock(CredentialsManager.class));

        return new NeighborsContentProviderClient(authority, cr,
                new DiskServicesAnalyzer(diskServicesScanner, credentialsManager,
                        pm, cr, mock(CommandStarter.class), mock(AutouploadCheckDebouncer.class)));
    }

    public static ImmutableContentBlock createContentBlock() {
        return createContentBlock("remote_id", 0);
    }

    public static ImmutableContentBlock createContentBlockWithOrder(final int order) {
        return createContentBlock(order, "remote_id", 0, FeedBlock.Status.INITIAL, 0);
    }

    public static ImmutableContentBlock createContentBlock(final String remoteId, final long revision) {
        return createContentBlock(0, remoteId, revision, FeedBlock.Status.INITIAL, 0);
    }

    public static ImmutableContentBlock createContentBlockWithStatus(final int status) {
        return createContentBlock(0, "remote_id", 0, status, 0);
    }

    public static ImmutableContentBlock createContentBlockWithStatusAndFilesCount(final int status, final int filesCount) {
        return createContentBlock(0, "remote_id", 0, status, filesCount);
    }

    private static ImmutableContentBlock createContentBlock(
        final int order,
        final String remoteId,
        final long revision,
        final int status,
        final int filesCount
    ) {
        return new ImmutableContentBlock(
            "test",
            "disk",
            filesCount,
            1000,
            2000,
            0,
            3000,
            order,
            status,
            null,
            null,
            null,
            remoteId,
            revision,
            "content_block",
            FeedBlock.DataSource.FEED,
            "index",
            "image"
        );
    }

    public static Credentials createCredentials() {
        return createCredentials("test");
    }

    public static Credentials createCredentials(final String accountName) {
        return new Credentials(accountName, 0L);
    }

    public static DiskUriProcessorMatcher createDiskUriProcessorMatcher(final Context context,
                                                                        @Nullable SQLiteOpenHelper2 db) {
        final SettingsDH settingsHelper = new SettingsDH(context);
        final SQLiteOpenHelper2 diskSqliteOpenHelper;
        if (db == null) {
            diskSqliteOpenHelper = new DH(context);
            createDiskDatabase(diskSqliteOpenHelper, null, new PathLock());
        } else {
            diskSqliteOpenHelper = db;
        }
        return new DiskUriProcessorMatcher(context, () -> diskSqliteOpenHelper, new QueueSqliteHelperProvider() {
            @Override
            public SQLiteOpenHelper2 getByUid(@Nullable Long uid) {
                return diskSqliteOpenHelper;
            }

            @Override
            public SQLiteOpenHelper2 getByUserName(String username) {
                return diskSqliteOpenHelper;
            }

            @Override
            public SQLiteOpenHelper2 getPhotoAccount() {
                return diskSqliteOpenHelper;
            }
        }, settingsHelper, null, null);
    }

    public static DownloadQueue createDownloadQueue(final Context context) {
        return new DownloadQueue(createDownloadQueueDatabase(context));
    }

    public static DownloadQueue createDownloadQueue(final DownloadQueueDatabase database) {
        return new DownloadQueue(database);
    }

    public static DownloadQueueDatabase createDownloadQueueDatabase(final Context context) {
        return new DownloadQueueDatabase(new DownloadQueueDH(context, 0L));
    }

    @NonNull
    public static DiskDatabase createDiskDatabase(final SQLiteOpenHelper2 sqlite,
                                                  @Nullable final ContentChangeNotifier notifier,
                                                  @Nullable final PathLock pathLockSpy) {
        final DiskDatabase diskDatabase = new DiskDatabase(sqlite, notifier, pathLockSpy);
        sqlite.addDatabaseOpenListener(new DiskTableSchemeCreator(false));
        sqlite.addDatabaseOpenListener(new UploadQueueTableCreator());
        sqlite.addDatabaseOpenListener(new DiskViewsCreator());
        sqlite.addDatabaseOpenListener(new PendingOperationsTableCreator());
        return diskDatabase;
    }

    @NonNull
    public static UploadQueue createUploadQueue(
        final NeighborsContentProviderClient allAuthoritiesContentProviderClient,
        final SelfContentProviderClient selfContentProviderClient,
        final SQLiteOpenHelper2 helper,
        final ContentResolver cr,
        final StorageListProvider storageListProviderStub,
        final Credentials creds
    ) {
        final Map<Integer, UploadQueueSerializer> serializers = getSerializers(
            allAuthoritiesContentProviderClient, selfContentProviderClient, creds, cr);
        return createUploadQueue(helper, cr, storageListProviderStub, serializers, creds);
    }

    public static Map<Integer, UploadQueueSerializer> getSerializers(
        final NeighborsContentProviderClient neighborsContentProviderClient,
        final SelfContentProviderClient selfContentProviderClient,
        final Credentials creds,
        final ContentResolver cr
    ) {
        final String userQueuePath = DiskContract.Queue.makeQueuePath(creds.getUser(), null);
        final FilesTimeExtractor filesTimeCalculator = new FilesTimeExtractor(cr);
        final Map<Integer, UploadQueueSerializer> serializers = new HashMap<>();

        final DiskQueueSerializer serializer = new DiskQueueSerializer(
            neighborsContentProviderClient,
            selfContentProviderClient,
            creds,
            userQueuePath,
            filesTimeCalculator,
            Collections.emptyList(),
            SystemClock.REAL
        );
        serializers.put(DiskContract.Queue.UploadItemType.AUTOUPLOAD, serializer);
        serializers.put(DiskContract.Queue.UploadItemType.DEFAULT, serializer);
        return serializers;
    }

    @NonNull
    public static UploadQueue createUploadQueue(final SQLiteOpenHelper2 helper,
                                                final ContentResolver cr,
                                                final StorageListProvider storageListProviderStub,
                                                final Map<Integer, UploadQueueSerializer> serializers,
                                                final Credentials creds) {
        helper.addDatabaseOpenListener(new DiskTableSchemeCreator(false));
        helper.addDatabaseOpenListener(new UploadQueueTableCreator());
        helper.addDatabaseOpenListener(new DiskViewsCreator());
        helper.addDatabaseOpenListener(new MediaItemsTableCreator());
        return new UploadQueue(helper, storageListProviderStub, serializers, creds);
    }

    public static UploadQueue createUploadQueueWithoutListeners(
        final NeighborsContentProviderClient neighborsContentProviderClient,
        final SelfContentProviderClient selfContentProviderClient,
        final SQLiteOpenHelper2 helper,
        final ContentResolver cr,
        final StorageListProvider storageListProviderStub,
        final Credentials creds) {
        final Map<Integer, UploadQueueSerializer> serializers = getSerializers(
            neighborsContentProviderClient, selfContentProviderClient, creds, cr);
        return createUploadQueueWithoutListeners(helper, cr, storageListProviderStub, serializers, creds);
    }

    private static UploadQueue createUploadQueueWithoutListeners(
        final SQLiteOpenHelper2 helper,
        final ContentResolver cr,
        final StorageListProvider storageListProviderStub,
        final Map<Integer, UploadQueueSerializer> serializers,
        final Credentials creds) {
        return new UploadQueue(helper, storageListProviderStub, serializers, creds);
    }

    public static ContentValues fileToContentValues(final String srcName,
                                                    final int mediaTypeCode,
                                                    final int state, final String destDir) {

        final ContentValues values = new ContentValues();
        values.put(MEDIA_TYPE_CODE, mediaTypeCode);
        values.put(DiskContract.Queue.SRC_NAME, srcName);
        values.put(DiskContract.Queue.DEST_NAME, srcName);
        values.put(DiskContract.Queue.DEST_DIR, destDir);
        values.put(DiskContract.Queue.DATE, System.currentTimeMillis());
        values.put(DiskContract.Queue.STATE, state);
        values.put(DiskContract.Queue.IS_DIR, 0);
        values.put(DiskContract.Queue.UPLOAD_ITEM_TYPE, DiskContract.Queue.UploadItemType.DEFAULT);

        return values;
    }

    public static ApplicationStorage createApplicationStorage(
        final Context context,
        final ApplicationSettings applicationSettings,
        final CredentialsManager credentialsManager,
        final CommandStarter commandStarter,
        final StorageListProvider storageListProvider,
        final Diagnostics diagnostics
    ) {
        final DeveloperSettings devSettings = mock(DeveloperSettings.class);
        when(devSettings.getMinLimitFreeSpaceMb()).thenReturn(100L);
        return new ApplicationStorage(context, applicationSettings,
                credentialsManager, commandStarter, storageListProvider,
                diagnostics, devSettings);
    }

    public static Storage createStorage(
        final Credentials credentials,
        final ApplicationStorage applicationStorage,
        final DiskDatabase diskDatabase,
        final DownloadQueue downloadQueue,
        final Storage.CacheStateObserver observer
    ) {
        return new Storage(credentials.getUid(), applicationStorage, diskDatabase, downloadQueue, observer);
    }

    public static LegacyPreviewsDatabase createLegacyPreviewsDatabase(final MomentsDatabase momentsDatabase, final SQLiteOpenHelper2 sqlite) {
        LegacyPreviewsDatabase legacyPreviewsDatabase = new LegacyPreviewsDatabase(momentsDatabase, sqlite);
        sqlite.addDatabaseOpenListener(new PreviewsDatabaseCreator());
        return legacyPreviewsDatabase;
    }

    public static TrashDatabase createTrashDatabase(final TrashDatabaseOpenHelper openHelper, final EventSender eventSender) {
        return createTrashDatabases(openHelper, null, eventSender).second;
    }

    public static OperationsDatabase createOperationsDatabase(final TrashDatabaseOpenHelper openHelper, final OperationsFactory operationsFactory) {
        return createTrashDatabases(openHelper, operationsFactory, null).first;
    }

    //These dbs should be created together
    public static Pair<OperationsDatabase, TrashDatabase> createTrashDatabases(
        final TrashDatabaseOpenHelper openHelper,
        final OperationsFactory operationsFactory,
        final EventSender eventSender
    ) {
        final TrashDatabase trashDatabase = new TrashDatabase(openHelper, eventSender);
        final OperationsDatabase operationsDatabase = new OperationsDatabase(openHelper, operationsFactory);

        openHelper.addDatabaseOpenListener(new OperationsSchemeCreator());
        openHelper.addDatabaseOpenListener(new TrashDatabaseSchemeCreator());
        openHelper.addDatabaseOpenListener(new TrashListProvider.TrashListSchemeCreator());

        return new Pair<>(operationsDatabase, trashDatabase);
    }

    public static MomentsDatabase createMomentsDatabase(SQLiteOpenHelper2 sqlite, @Nullable SharedPreferences preferences) {
        MomentsDatabase momentsDatabase = new MomentsDatabase(sqlite, preferences, new TableSyncSuffixesCreator(preferences).create());
        sqlite.addDatabaseOpenListener(new MomentsDatabaseSchemeCreator(() -> preferences));
        return momentsDatabase;
    }

    public static UserSettings createUserSettings(final String userName, final Settings settings) {
        return new UserSettings(
            settings,
            mock(EventSender.class),
            userName,
            SystemClock.REAL,
            mock(BackgroundWatcher.class)
        );
    }
}
