/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import com.google.common.io.ByteStreams;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.helpers.BaseDatabaseHelper;
import com.yandex.datasync.internal.database.helpers.DatabasesDatabaseHelper;
import com.yandex.datasync.internal.database.helpers.SnapshotDatabaseHelper;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.yandex.datasync.internal.database.helpers.SnapshotDatabaseHelper.DATABASE_NAME_FORMAT;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DatabaseManagerTest {

    private static final int THREADS_COUNT = 100;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_DATABASE_ID_2 = "mock_database_id_2";

    @Test
    public void testMasterSameContext() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        final SQLiteDatabaseWrapper expected = databaseManager.openDatabaseWrapped(YDSContext.APP);
        final SQLiteDatabaseWrapper actual = databaseManager.openDatabaseWrapped(YDSContext.APP);

        assertNotNull(expected);
        assertNotNull(actual);

        assertThat(actual, is(expected));
    }

    @Test
    public void testMasterNotSameContext() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        final SQLiteDatabaseWrapper expected = databaseManager.openDatabaseWrapped(YDSContext.APP);
        final SQLiteDatabaseWrapper actual = databaseManager.openDatabaseWrapped(YDSContext.USER);

        assertNotNull(expected);
        assertNotNull(actual);

        assertThat(actual, is(not(expected)));
    }

    @Test
    public void testSameContextNotSameDatabaseId() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final SQLiteDatabaseWrapper expected = databaseManager.openDatabaseWrapped(YDSContext.APP,
                                                                                   MOCK_DATABASE_ID);
        final SQLiteDatabaseWrapper actual = databaseManager.openDatabaseWrapped(YDSContext.APP,
                                                                                 MOCK_DATABASE_ID_2);

        assertNotNull(expected);
        assertNotNull(actual);

        assertThat(actual, is(not(expected)));
    }

    @Test
    public void testSameContextSameDatabaseId() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final SQLiteDatabaseWrapper expected = databaseManager.openDatabaseWrapped(YDSContext.APP,
                                                                                   MOCK_DATABASE_ID);
        final SQLiteDatabaseWrapper actual = databaseManager.openDatabaseWrapped(YDSContext.USER,
                                                                                 MOCK_DATABASE_ID);

        assertNotNull(expected);
        assertNotNull(actual);

        assertThat(actual, is(not(expected)));
    }

    @Test
    public void testNotSameContextNotSameDatabaseId() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final SQLiteDatabaseWrapper expected = databaseManager.openDatabaseWrapped(YDSContext.APP,
                                                                                   MOCK_DATABASE_ID);
        final SQLiteDatabaseWrapper actual = databaseManager.openDatabaseWrapped(YDSContext.USER,
                                                                                 MOCK_DATABASE_ID_2);

        assertNotNull(expected);
        assertNotNull(actual);

        assertThat(actual, is(not(expected)));
    }

    @Test
    public void testNotSameContextSameDatabaseId() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final SQLiteDatabaseWrapper expected = databaseManager.openDatabaseWrapped(YDSContext.APP,
                                                                                   MOCK_DATABASE_ID);
        final SQLiteDatabaseWrapper actual = databaseManager.openDatabaseWrapped(YDSContext.USER,
                                                                                 MOCK_DATABASE_ID);

        assertNotNull(expected);
        assertNotNull(actual);

        assertThat(actual, is(not(expected)));
    }

    @Test
    public void testMasterAppContextDatabaseName() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final BaseDatabaseHelper database = databaseManager.getDatabaseHelper(YDSContext.APP);

        final String databaseLocation = database.getDatabaseName();

        final String expected = String.format(DatabasesDatabaseHelper.DATABASE_NAME_FORMAT,
                                              YDSContext.APP,
                                              MOCK_USER_ID);
        assertThat(databaseLocation, is(equalToIgnoringCase(expected)));
    }

    @Test
    public void testMasterUserContextDatabaseName() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final BaseDatabaseHelper database = databaseManager.getDatabaseHelper(YDSContext.USER);

        final String databaseLocation = database.getDatabaseName();

        final String expected = String.format(DatabasesDatabaseHelper.DATABASE_NAME_FORMAT,
                                              YDSContext.USER,
                                              MOCK_USER_ID);
        assertThat(databaseLocation, is(equalToIgnoringCase(expected)));
    }

    @Test
    public void testAppContextDatabaseIdDatabaseName() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final BaseDatabaseHelper database = databaseManager.getDatabaseHelper(YDSContext.APP,
                                                                              MOCK_DATABASE_ID);

        final String databaseLocation = database.getDatabaseName();

        final String expected = String.format(DATABASE_NAME_FORMAT,
                                              YDSContext.APP,
                                              MOCK_DATABASE_ID,
                                              MOCK_USER_ID);
        assertThat(databaseLocation, is(equalToIgnoringCase(expected)));
    }

    @Test
    public void testUserContextDatabaseIdDatabaseName() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final BaseDatabaseHelper database = databaseManager.getDatabaseHelper(YDSContext.USER,
                                                                              MOCK_DATABASE_ID);

        final String databaseLocation = database.getDatabaseName();

        final String expected = String.format(DATABASE_NAME_FORMAT,
                                              YDSContext.USER,
                                              MOCK_DATABASE_ID,
                                              MOCK_USER_ID);
        assertThat(databaseLocation, is(equalToIgnoringCase(expected)));
    }

    @Test
    public void testClose() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));
        databaseManager.init(MOCK_USER_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);

        verify(databaseManager).getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID);
        databaseManager.close(YDSContext.USER, MOCK_DATABASE_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);
        verify(databaseManager, times(2)).getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID);
    }

    @Test
    public void testCloseAll() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));
        databaseManager.init(MOCK_USER_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);

        verify(databaseManager).getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID);
        databaseManager.closeAll();

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);
        verify(databaseManager, times(2)).getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID);
    }


    @Test
    public void testUpgrade() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));

        databaseManager.init(MOCK_USER_ID);
        when(databaseManager.getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID, false)).thenReturn(setupOldDatabase());
        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);
        int tableCount;
        try (SQLiteDatabase oldDb =
                     databaseManager.getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID).getWritableDatabase();
             Cursor cursorView =
                     oldDb.rawQuery("SELECT * FROM sqlite_master WHERE type='view'; ", null);
             Cursor cursorTables =
                     oldDb.rawQuery("SELECT * FROM sqlite_master WHERE type='table'; ", null)) {
            assertEquals("Version should be 1", 1, oldDb.getVersion());
            assertEquals("View already exists", 1, cursorView.getCount());
            tableCount = cursorTables.getCount();
            assertTrue("No tables found", tableCount > 0);

        }

        when(databaseManager.getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID, false))
                .thenReturn(new SnapshotDatabaseHelper(RuntimeEnvironment.application,
                                                       YDSContext.USER,
                                                       MOCK_DATABASE_ID,
                                                       MOCK_USER_ID));
        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);

        try (SQLiteDatabase newDb =
                     databaseManager.getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID).getWritableDatabase();
             Cursor cursor =
                     newDb.rawQuery("SELECT * FROM sqlite_master WHERE type='view'; ", null);
             Cursor cursorTables =
                     newDb.rawQuery("SELECT * FROM sqlite_master WHERE type='table'; ", null)) {
            assertEquals("Version should be 1",2, newDb.getVersion());
            assertEquals("View doesn't exists",2, cursor.getCount());
            assertEquals("Should be same count of tables", tableCount, cursorTables.getCount());
        }
    }

    private BaseDatabaseHelper setupOldDatabase() {
        try {
            final String dbName = String.format(DATABASE_NAME_FORMAT,
                                                YDSContext.USER.name(),
                                                MOCK_DATABASE_ID,
                                                MOCK_USER_ID);
            Application context = RuntimeEnvironment.application;
            final File databasePath = context.getDatabasePath(dbName);
            databasePath.getParentFile().mkdirs();
            databasePath.createNewFile();
            ByteStreams.copy(getClass().getResourceAsStream("/olddatabases/" + dbName),
                             new FileOutputStream(databasePath));
            return new BaseDatabaseHelper(context, dbName, null, 1);
        } catch (IOException e) {
            throw new RuntimeException("Can't load db", e);
        }
    }

    @Test
    public void testCache() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));
        databaseManager.init(MOCK_USER_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);

        verify(databaseManager).getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID);
        databaseManager.close(YDSContext.USER, MOCK_DATABASE_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);
        verify(databaseManager, times(2)).getDatabaseHelper(YDSContext.USER, MOCK_DATABASE_ID);
    }

    @Test
    public void testMasterClose() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));
        databaseManager.init(MOCK_USER_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER);

        verify(databaseManager).getDatabaseHelper(YDSContext.USER);
        databaseManager.close(YDSContext.USER, null);

        databaseManager.openDatabaseWrapped(YDSContext.USER);
        verify(databaseManager, times(2)).getDatabaseHelper(YDSContext.USER);
    }

    @Test
    public void testMasterCloseAll() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));
        databaseManager.init(MOCK_USER_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER);

        verify(databaseManager).getDatabaseHelper(YDSContext.USER);
        databaseManager.closeAll();

        databaseManager.openDatabaseWrapped(YDSContext.USER);
        verify(databaseManager, times(2)).getDatabaseHelper(YDSContext.USER);
    }

    @Test
    public void testMasterCache() {
        final DatabaseManager databaseManager =
                spy(new DatabaseManager(RuntimeEnvironment.application));
        databaseManager.init(MOCK_USER_ID);

        databaseManager.openDatabaseWrapped(YDSContext.USER);

        verify(databaseManager).getDatabaseHelper(YDSContext.USER);
        databaseManager.close(YDSContext.USER, null);

        databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID);
        verify(databaseManager, times(1)).getDatabaseHelper(YDSContext.USER);
    }

    @Test
    public void testDelete() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final BaseDatabaseHelper databaseHelper =
                databaseManager.getDatabaseHelper(YDSContext.APP, MOCK_DATABASE_ID);
        final SQLiteDatabase database = databaseHelper.getWritableDatabase();
        assertNotNull(database);
        final String databaseName = databaseHelper.getDatabaseName();
        final File databaseFile = RuntimeEnvironment.application.getDatabasePath(databaseName);

        assertTrue(databaseFile.exists());

        databaseManager.delete(YDSContext.APP, MOCK_DATABASE_ID);

        assertFalse(databaseFile.exists());
    }

    @Test
    public void testHasDatabase() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final BaseDatabaseHelper databaseHelper =
                databaseManager.getDatabaseHelper(YDSContext.APP, MOCK_DATABASE_ID);
        final SQLiteDatabase database = databaseHelper.getWritableDatabase();
        assertNotNull(database);

        final boolean exists = databaseManager.hasDatabase(YDSContext.APP, MOCK_DATABASE_ID);
        assertTrue(exists);
    }

    @Test
    public void testHasNotDatabase() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        final boolean exists = databaseManager.hasDatabase(YDSContext.APP, MOCK_DATABASE_ID);
        assertFalse(exists);
    }

    @Test
    public void testMultithreadedAddCloseAll() throws InterruptedException {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        databaseManager.openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID);

        final CountDownLatch lock = new CountDownLatch(THREADS_COUNT * 3);

        new Thread(() -> {
            for (int i = 0; i < THREADS_COUNT; i++) {
                final String databaseId = "database_id" + i;
                new Thread(new OpenRunnable(lock, databaseManager, databaseId)).start();
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < THREADS_COUNT; i++) {
                final String databaseId = "database_id" + i;
                new Thread(new CloseRunnable(lock, databaseManager, databaseId)).start();
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < THREADS_COUNT; i++) {
                new Thread(new CloseAllRunnable(lock, databaseManager)).start();
            }
        }).start();

        lock.await(10, TimeUnit.SECONDS);
        assertThat(lock.getCount(), is(0L));
    }

    private class CloseRunnable implements Runnable {

        @NonNull
        private final DatabaseManager databaseManager;

        @NonNull
        private final String databaseId;

        @NonNull
        private final CountDownLatch lock;

        private CloseRunnable(@NonNull final CountDownLatch lock,
                              @NonNull final DatabaseManager databaseManager,
                              @NonNull final String databaseId) {
            this.lock = lock;
            this.databaseManager = databaseManager;
            this.databaseId = databaseId;
        }

        @Override
        public void run() {
            databaseManager.close(YDSContext.APP, databaseId);
            lock.countDown();
        }
    }


    private class CloseAllRunnable implements Runnable {

        @NonNull
        private final DatabaseManager databaseManager;

        @NonNull
        private final CountDownLatch lock;

        private CloseAllRunnable(@NonNull final CountDownLatch lock,
                                 @NonNull final DatabaseManager databaseManager) {
            this.lock = lock;
            this.databaseManager = databaseManager;
        }

        @Override
        public void run() {
            databaseManager.closeAll();
            lock.countDown();
        }
    }


    private class OpenRunnable implements Runnable {

        @NonNull
        private final DatabaseManager databaseManager;

        @NonNull
        private final String databaseId;

        @NonNull
        private final CountDownLatch lock;

        private OpenRunnable(@NonNull final CountDownLatch lock,
                             @NonNull final DatabaseManager databaseManager,
                             @NonNull final String databaseId) {
            this.lock = lock;
            this.databaseManager = databaseManager;
            this.databaseId = databaseId;
        }

        @Override
        public void run() {
            databaseManager.openDatabaseWrapped(YDSContext.APP, databaseId);
            lock.countDown();
        }
    }
}