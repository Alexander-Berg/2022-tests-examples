package ru.yandex.yandexmaps.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import ru.yandex.yandexmaps.common.database.DatabaseOpenHelper;
import ru.yandex.yandexmaps.guidance.annotations.DatabaseModule;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DatabaseTest {

    private DatabaseOpenHelper openHelper;

    @Before
    public void setUp() {
        final DatabaseModule module = new DatabaseModule();
        openHelper = module.openHelper(RuntimeEnvironment.application);
    }

    @After
    public void tearDown() {
        RuntimeEnvironment.application.deleteDatabase(DatabaseModule.Companion.getDATABASE_NAME());
    }

    @Test
    public void creationSuccessful() {
        openHelper.getWritableDatabase().rawQuery("SELECT * FROM sqlite_master", new String[] {});
    }
}