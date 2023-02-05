package ru.yandex.disk.photoslice;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.FileItem;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.sql.SQLiteDatabase2;
import ru.yandex.disk.sql.TableSuffix;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.MediaTypes;
import ru.yandex.util.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.disk.photoslice.PhotosliceTestHelper.newMomentBuilder;

@Config(manifest = Config.NONE)
public class MomentsDatabaseTest extends AndroidTestCase2 {

    private MomentsDatabase database;
    private SQLiteOpenHelper2 dbOpenHelper;
    private DiskDatabase diskDatabase;
    private SharedPreferences prefs;

    private static final String MOCK_ETAG = "testEtag";
    private static final String MOCK_FILE_NAME = "photo";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        dbOpenHelper = TestObjectsFactory.createSqlite(getMockContext());
        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpenHelper);
        prefs = PreferenceManager.getDefaultSharedPreferences(getMockContext());
        database = TestObjectsFactory.createMomentsDatabase(dbOpenHelper, prefs);
    }

    @Test
    public void shouldQueryMoment() throws Exception {
        Moment origin = Moment.Builder.newBuilder()
                .setItemsCount(10)
                .setLocalityEn("Moscow")
                .setLocalityRu("Москва")
                .setLocalityTr("Moskova")
                .setLocalityUk("Москва")
                .setSyncId("momentId")
                .setFromDate(100L)
                .setToDate(200L)
                .setIsInited(false)
                .build();
        database.insertOrReplace(origin);

        MomentCursor moments = database.queryNotInitedSyncingMoments();

        Moment moment = Moment.Builder.copyOf(moments.get(0));
        assertThat(moment, equalTo(origin));
    }

    @Test
    public void shouldQueryMomentItemsCount() throws Exception {
        database.insertOrReplace(newMomentBuilder().setSyncId("momentId").build());
        database.insertOrReplace("momentId", new MomentItemMapping("momentItemId", "/disk/photo"));

        assertEquals(1, database.getMomentsItemsCount());

        database.insertOrReplace(newMomentBuilder().setSyncId("momentId2").build());
        database.insertOrReplace("momentId2", new MomentItemMapping("momentItemId2", "/disk/photo2"));

        database.insertOrReplace("momentId2", new MomentItemMapping("momentItemId3", "/disk/photo3"));

        assertEquals(3, database.getMomentsItemsCount());
    }

    @Test
    public void shouldQueryMomentItem() throws Exception {
        database.insertOrReplace(newMomentBuilder().setSyncId("momentId").build());
        database.insertOrReplace("momentId", new MomentItemMapping("momentItemId", "/disk/photo"));

        MomentItemMappingCursor momentItems = database.queryMomentItemMappings();
        assertThat(momentItems.getCount(), equalTo(1));

        MomentItemMapping momentItem = momentItems.singleAndCopy();
        assertThat(momentItem.getSyncId(), equalTo("momentItemId"));
    }

    @Test
    public void shouldReplaceMoment() throws Exception {
        Moment.Builder builder = newMomentBuilder().setSyncId("momentId");
        database.insertOrReplace(builder.setItemsCount(1).build());
        database.insertOrReplace(builder.setItemsCount(2).build());

        MomentCursor moments = database.queryNotInitedSyncingMoments();
        assertThat(moments.getCount(), equalTo(1));

        Moment moment = moments.get(0);
        assertThat(moment.getItemsCount(), equalTo(2));
    }

    @Test
    public void shouldReplaceMomentItem() throws Exception {
        database.insertOrReplace(newMomentBuilder().setSyncId("momentId").build());
        database.insertOrReplace("momentId", new MomentItemMapping("momentItemId", "/disk/old"));
        database.insertOrReplace("momentId", new MomentItemMapping("momentItemId", "/disk/new"));

        MomentItemMappingCursor momentItems = database.queryMomentItemMappings();
        assertThat(momentItems.getCount(), equalTo(1));

        MomentItemMapping momentItem = momentItems.singleAndCopy();
        assertThat(momentItem.getPath(), equalTo("/disk/new"));
    }

    @Test
    public void shouldProviderDataFromOldMomentTablesAfterBeginSync() throws Exception {
        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());

        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("2").build());

        assertThat(database.queryReadyMoments().getCount(), equalTo(1));
    }

    @Test
    public void shouldProviderNewDataMomentTablesAfterSuccessfulEnd() throws Exception {
        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());
        database.setSyncSuccessful();
        database.endSync();

        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("2").build());
        database.setSyncSuccessful();
        database.endSync();

        assertThat(database.queryReadyMoments().getCount(), equalTo(2));
    }

    @Test
    public void shouldNotProviderNewDataMomentTablesAfterUnsuccessfulEnd() throws Exception {
        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());

        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("2").build());
        //database.setSyncSuccessful();
        database.endSync();

        assertThat(database.queryReadyMoments().getCount(), equalTo(1));
    }

    @Test
    public void shouldResetTableAfterFail() throws Exception {
        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());
        //database.setSyncSuccessful();
        database.endSync();

        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("2").build());
        database.setSyncSuccessful();
        database.endSync();

        assertThat(database.queryReadyMoments().getCount(), equalTo(1));
    }

    @Test
    public void shouldHandleDoubleSync() throws Exception {
        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());
        database.setSyncSuccessful();
        database.endSync();

        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("2").build());
        database.setSyncSuccessful();
        database.endSync();

        assertThat(database.queryReadyMoments().getCount(), equalTo(2));
    }

    @Test
    public void shouldPersistActiveTable() throws Exception {
        database.beginSync();
        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());
        database.setSyncSuccessful();
        database.endSync();

        SQLiteOpenHelper2 dbOpenHelper = TestObjectsFactory.createSqlite(getMockContext());
        TestObjectsFactory.createDiskDatabase(dbOpenHelper);
        database = TestObjectsFactory.createMomentsDatabase(dbOpenHelper,
                PreferenceManager.getDefaultSharedPreferences(getMockContext()));

        assertThat(database.queryReadyMoments().getCount(), equalTo(1));
    }

    @Test
    public void shouldUseSyncingTableForQuerySyncingData() throws Exception {
        Moment.Builder momentsBuilder = newMomentBuilder();
        database.insertOrReplace(momentsBuilder.setSyncId("1").build());
        database.insertOrReplace("1", new MomentItemMapping("1", "/disk/a"));
        database.setSyncSuccessful();
        database.endSync();

        database.beginSync();

        database.insertOrReplace(momentsBuilder.setSyncId("2").build());
        database.insertOrReplace("2", new MomentItemMapping("1", "/disk/b"));

        assertThat(database.queryNotInitedSyncingMoments().getCount(), equalTo(2));
        assertThat(database.querySyncingMomentPlacesColumns("2").getCount(), equalTo(1));
        assertThat(database.querySyncingMomentItemMappings().getCount(), equalTo(2));
    }

    @Test
    public void shouldResetSuccessfulFlag() throws Exception {
        database.beginSync();

        database.insertOrReplace(newMomentBuilder().setSyncId("1").build());

        database.setSyncSuccessful();
        database.endSync();

        database.beginSync();

        database.deleteMoment("1");

        //database.setSyncSuccessful();
        database.endSync();

        assertThat(database.queryReadyMoments().getCount(), equalTo(1));
    }

    @Test
    public void shouldClearAllTables() throws Exception {
        Moment.Builder momentsBuilder = newMomentBuilder();
        database.insertOrReplace(momentsBuilder.setSyncId("1").build());

        database.beginSync();
        database.insertOrReplace(momentsBuilder.setSyncId("2").build());

        database.clear();

        assertThat(database.queryNotInitedSyncingMoments().getCount(), equalTo(0));
        assertThat(database.queryReadyMoments().getCount(), equalTo(0));
        assertThat(database.queryMomentItemMappings().getCount(), equalTo(0));
        SQLiteDatabase2 db = dbOpenHelper.getWritableDatabase();
        String table = TableSuffix.FIRST.getTableName(MomentsDatabase.TABLE_MOMENT_TO_MOMENT_ITEM);
        assertThat(db.query(table, null, null, null, null, null, null).getCount(), equalTo(0));
    }

    @Test
    public void shouldUpdateMomentToMomentItemPathOnFileRename() throws Exception {
        final String itemPath = mockSingleFileInDiskAndMomentToItem();

        diskDatabase.renameFile(new Path(itemPath), "photo2");

        assertSingleMomentItemMappingPath("/disk/photo2");
    }

    @Test
    public void shouldUpdateMomentToMomentItemPathOnFileMove() throws Exception {
        final String itemPath = mockSingleFileInDiskAndMomentToItem();

        final String moveToFolderPath = "/disk/newFolder";
        diskDatabase.moveFile(new Path(itemPath), new Path(moveToFolderPath), FileItem.OfflineMark.NOT_MARKED);

        assertSingleMomentItemMappingPath(moveToFolderPath + "/" + MOCK_FILE_NAME);
    }

    @Test
    public void shouldSortMomentsByEndDate() {
        Moment.Builder momentsBuilder = newMomentBuilder();
        database.insertOrReplace(momentsBuilder.setSyncId("1").setToDate(1).build());
        database.insertOrReplace(momentsBuilder.setSyncId("2").setToDate(2).build());

        final MomentCursor moments = database.queryNotInitedSyncingMoments();
        assertThat(moments.getCount(), equalTo(2));
        assertThat(moments.get(0).getSyncId(), equalTo("2"));
        assertThat(moments.get(1).getSyncId(), equalTo("1"));
    }

    @Test
    public void shouldReturnTrueIfFoundItemsByEtag() {
        mockSingleFileInDiskAndMomentToItem();

        assertThat(database.containsItems(Collections.singletonList(MOCK_ETAG)), equalTo(true));
    }

    @Test
    public void shouldReturnFlaseIfNotFoundItemByEtag() {
        assertThat(database.containsItems(Collections.singletonList(MOCK_ETAG)), equalTo(false));
    }

    @Test
    public void shouldHandleManyArguments() {
        final ArrayList<String> etags = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            etags.add(String.valueOf(i));
        }
        assertThat(database.containsItems(etags), equalTo(false));
    }

    @Test
    public void shouldDeleteItemsRecentlyUploadedBefore() {
        long now = System.currentTimeMillis();

        database.insertRecentlyUploadedItem(new Path("/disk", "1"), now);
        database.insertRecentlyUploadedItem(new Path("/disk", "2"), now + 1);
        database.insertRecentlyUploadedItem(new Path("/disk", "3"), now - 1);

        assertThat(database.queryMomentItemMappings().copyToList(), hasSize(3));

        database.deleteRecentlyUploadedItemsUploadedBefore(true, now + 1);

        List<String> paths = Lists.transform(
                database.queryMomentItemMappings().copyToList(), MomentItemMapping::getPath);

        assertThat(paths, equalTo(Collections.singletonList("/disk/2")));
    }

    @Test
    public void shouldDeleteRecentlyUploadedAndObtainedItems() {
        long now = System.currentTimeMillis();

        Path found = new Path("/disk", "found");
        Path notFound = new Path("/disk", "not-found");
        Path notUploaded = new Path("/disk", "not-uploaded");

        for (Path path : Arrays.asList(found, notFound, notUploaded)) {
            diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(path).build());
        }

        database.insertRecentlyUploadedItem(found, now + 1);
        database.insertRecentlyUploadedItem(notFound, now + 2);

        for (Path path : Arrays.asList(found, notUploaded)) {
            database.insertOrReplace("moment-id", new MomentItemMapping(path.getName(), path.getPath()));
        }
        assertThat(database.queryMomentItemMappings().copyToList(), hasSize(4));

        database.deleteRecentlyUploadedItemsObtainedFromServer();

        List<String> syncIds = Lists.transform(
                database.queryMomentItemMappings().copyToList(), MomentItemMapping::getSyncId);

        assertThat(syncIds, equalTo(Arrays.asList(
                notUploaded.getName(), found.getName(), Long.toString(now + 2))));
    }

    @Test
    public void shouldCopyRecentlyUploadedItems() {
        Path copied = new Path("/disk", "copied");
        Path duplicate = new Path("/disk", "duplicate");

        database.insertOrReplace("moment-id", new MomentItemMapping("/disk/left", "left"));

        database.insertRecentlyUploadedItem(copied, 1);

        imitatePhotosliceInit();

        database.insertRecentlyUploadedItem(duplicate, 2);

        database.copyRecentlyUploadedItemMappings();

        List<String> paths = Lists.transform(
                database.querySyncingMomentItemMappings().copyToList(), MomentItemMapping::getPath);

        assertThat(paths, equalTo(Arrays.asList(duplicate.getPath(), copied.getPath())));
    }

    private void imitatePhotosliceInit() {
        prefs.edit()
                .putLong(DiskContract.PHOTOSLICE_UPDATE_TIME, 123)
                .apply();
        database.getTableSuffixes().reset();
    }

    private void assertSingleMomentItemMappingPath(final String expectedPath) {
        try (final MomentItemMappingCursor momentItemMappings = database.queryMomentItemMappings()) {
            if (momentItemMappings.moveToFirst()) {
                assertEquals(expectedPath, momentItemMappings.getPath());
            } else {
                fail("No items in moments cursor");
            }
        }
    }

    @NonNull
    private String mockSingleFileInDiskAndMomentToItem() {
        final String itemPath = "/disk/" + MOCK_FILE_NAME;
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(itemPath)
                .setMediaType(MediaTypes.IMAGE).setEtag(MOCK_ETAG).build());
        database.insertOrReplace(newMomentBuilder().setSyncId("momentId").build());
        database.insertOrReplace("momentId", new MomentItemMapping("momentItemId", itemPath));
        return itemPath;
    }

}
