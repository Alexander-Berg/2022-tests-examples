package ru.yandex.disk.provider;

import android.database.Cursor;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.database.Databases;
import ru.yandex.disk.test.AndroidTestCase2;

import javax.annotation.NonnullByDefault;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.SettingsDH.DatabaseVersions.VERSION_5_12;
import static ru.yandex.disk.util.Arrays2.asStringArray;

@NonnullByDefault
@Config(manifest = Config.NONE)
public class SettingsUpdate306Test extends AndroidTestCase2 {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Databases.recreateDbFromTestResources(getMockContext(), DiskContract.Settings.DATABASE_NAME,
                "/olddatabases/SETTING_1.31_CORRUPTED");
    }

    @Test
    public void shouldUpdate306() throws Exception {
        final SettingsDH db = new SettingsDH(getMockContext());
        assertThatContains_DATA(db, 1);
    }

    private void assertThatContains_DATA(final SettingsDH db, final int count) {
        try (final Cursor cursor = db.getReadableDatabase()
                .query(DiskContract.Settings.TABLE_NAME, null,
                        "NAME = ?",
                        asStringArray("DATA"),
                        null, null, null)) {

            assertThat(cursor.getCount(), equalTo(count));
        }
    }
}
