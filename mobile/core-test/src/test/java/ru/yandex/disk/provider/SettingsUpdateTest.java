package ru.yandex.disk.provider;

import android.content.ContentValues;
import android.database.Cursor;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.database.Databases;
import ru.yandex.disk.sql.DbUtils;
import ru.yandex.disk.test.AndroidTestCase2;

import javax.annotation.NonnullByDefault;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.DiskContract.Settings.*;
import static ru.yandex.disk.util.Arrays2.asStringArray;

@NonnullByDefault
@Config(manifest = Config.NONE)
public class SettingsUpdateTest extends AndroidTestCase2 {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Databases.recreateDbFromTestResources(getMockContext(), DiskContract.Settings.DATABASE_NAME,
                "/olddatabases/SETTING_1.31");
    }

    @Test
    public void shouldDeleteUnusedSettingsForUser() throws Exception {
        final SettingsDH db = new SettingsDH(getMockContext());
        assertThat_CORRECT_CACHE_SWITCH_Removed(db);
        assertThatOtherSettingsNotAffected(db);
    }

    @Test
    public void shouldUpdateOnly_270() throws Exception {
        final SettingsDH db_271 = new SettingsDH(getMockContext());
        final ContentValues cv = new ContentValues();
        cv.put("NAME", "CORRECT_CACHE_SWITCH");
        db_271.getWritableDatabase().insert(DiskContract.Settings.TABLE_NAME, null, cv);

        db_271.close();

        final SettingsDH db = new SettingsDH(getMockContext());
        assertThatContains_CORRECT_CACHE_SWITCH(db, 1);
    }

    @Test
    public void mustRecreateTableWithNewPrimaryKeyOn304Update() {
        final SettingsDH db = new SettingsDH(getMockContext());
        assertThatTableRecreatedWithNewPrimaryKey(db);
    }

    @Test
    public void mustSaveDataOnTableRecreation() {
        final SettingsDH db = new SettingsDH(getMockContext());
        assertThatDataNotLost(db);
    }

    @Test
    public void mustClearOldColumnOnUpdateCredentialsTable() {
        final SettingsDH db = new SettingsDH(getMockContext());
        assertThatCredentialsRecreatedRight(db);
    }

    private void assertThatCredentialsRecreatedRight(final SettingsDH db) {
        assertRightTableScheme(db, CredentialsContract.TABLE_NAME, "CREATE TABLE \"CREDENTIAL\" "
                + "(USER TEXT, TOKEN TEXT DEFAULT NULL, IS_LOGGED INTEGER DEFAULT -1, "
                + "LAST_TIME_LOGGED_IN INTEGER DEFAULT 0, UID TEXT, IS_SELECTED INTEGER DEFAULT 0, "
                + "IS_PHOTO INTEGER DEFAULT 0)");
    }

    private void assertThatDataNotLost(final SettingsDH db) {
        try (final Cursor cursor = db.getReadableDatabase()
                .query(DiskContract.Settings.TABLE_NAME, asStringArray(SCOPE, NAME, VALUE),
                        null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                assertThat(cursor.getString(0), equalTo("user@testuser"));
                assertThat(cursor.getString(1), equalTo("BITMAP_CACHE_SIZE"));
                assertThat(cursor.getString(2), equalTo("1000"));
            } else {
                fail("Data not saved on update!");
            }
        }
    }

    private void assertThatTableRecreatedWithNewPrimaryKey(final SettingsDH db) {
        assertRightTableScheme(db, "PLAIN_SETTINGS_TABLE", "CREATE TABLE \"PLAIN_SETTINGS_TABLE\" "
                + "(SCOPE TEXT DEFAULT 'ALL', NAME TEXT, VALUE TEXT,  PRIMARY KEY (SCOPE, NAME) )");
    }

    private void assertThatContains_CORRECT_CACHE_SWITCH(final SettingsDH db, final int count) {
        try (final Cursor cursor = db.getReadableDatabase()
                .query(DiskContract.Settings.TABLE_NAME, null,
                        "NAME = ?",
                        asStringArray("CORRECT_CACHE_SWITCH"),
                        null, null, null)) {

            assertThat(cursor.getCount(), equalTo(count));
        }
    }

    private void assertThat_CORRECT_CACHE_SWITCH_Removed(final SettingsDH db) {
        try (final Cursor cursor = db.getReadableDatabase()
                .query(DiskContract.Settings.TABLE_NAME, null,
                        "SCOPE = ? AND NAME = ?",
                        asStringArray("user@testuser", "CORRECT_CACHE_SWITCH"),
                        null, null, null)) {

            assertThat(cursor.getCount(), equalTo(0));
        }
    }

    private void assertThatOtherSettingsNotAffected(final SettingsDH db) {
        assertThatContainsCount(db, equalTo(2));
    }

    private void assertThatContainsCount(final SettingsDH db, final Matcher<Integer> matcher) {
        try (final Cursor otherSetting = db.getReadableDatabase()
                .query(DiskContract.Settings.TABLE_NAME, null, null, null, null, null, null)) {
            assertThat(otherSetting.getCount(), matcher);
        }
    }

    private void assertRightTableScheme(final SettingsDH db,
                                        final String tableName,
                                        final String scheme) {
        final String settingsTableCreateScript =
                DbUtils.getTableSchemaSql(db.getReadableDatabase(), tableName);

        if (settingsTableCreateScript != null) {
            assertThat(settingsTableCreateScript, equalTo(scheme));
        } else {
            fail("No database creation found!");
        }

    }

}
