/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql;

import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.Rows.ID;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.Rows.INTERNAL_FIELD_ID;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.Rows.TYPE;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.Rows.VALUE;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.TABLE_NAME;
import static com.yandex.datasync.internal.util.Arrays2.asStringArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class SQLiteDatabaseWrapperTest {

    private static final String MOCK_DATA_TYPE = "mock_data_type";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATA_VALUE = "mock_data_value";

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final int INSERTS_COUNT = 100;

    private static final int THREADS_COUNT = 10;

    private DatabaseManager databaseManager;

    private CountDownLatch lock;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test
    public void testMultithreadingInsert() throws InterruptedException {
        lock = new CountDownLatch(THREADS_COUNT * 2);

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new WriteRunnable(YDSContext.APP)).start();
        }

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new WriteRunnable(YDSContext.USER)).start();
        }

        lock.await(10, TimeUnit.SECONDS);

        assertThat(lock.getCount(), is(0L));

        final Cursor appCursor =
                databaseManager.openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(appCursor.getCount(), is(THREADS_COUNT * INSERTS_COUNT));

        final Cursor userCursor =
                databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(userCursor.getCount(), is(THREADS_COUNT * INSERTS_COUNT));
    }

    @Test
    public void testMultithreadingQuery() throws InterruptedException {
        lock = new CountDownLatch(THREADS_COUNT * 2 + 2);

        new Thread(new WriteRunnable(YDSContext.APP)).start();
        new Thread(new WriteRunnable(YDSContext.USER)).start();

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new QueryRunnable(YDSContext.APP)).start();
        }

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new QueryRunnable(YDSContext.USER)).start();
        }

        lock.await(10, TimeUnit.SECONDS);

        final Cursor appCursor =
                databaseManager.openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(appCursor.getCount(), is(INSERTS_COUNT));

        final Cursor userCursor =
                databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(userCursor.getCount(), is(INSERTS_COUNT));
    }

    @Test
    public void testMultithreadingDelete() throws InterruptedException {
        lock = new CountDownLatch(THREADS_COUNT * 2 + 2);

        new Thread(new WriteRunnable(YDSContext.APP)).run();

        new Thread(new WriteRunnable(YDSContext.USER)).run();

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new DeleteRunnable(YDSContext.APP, i + 1)).start();
        }

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new DeleteRunnable(YDSContext.USER, i + 1)).start();
        }

        lock.await(10, TimeUnit.SECONDS);

        final Cursor appCursor =
                databaseManager.openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(INSERTS_COUNT - THREADS_COUNT, is(appCursor.getCount()));

        final Cursor userCursor =
                databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(userCursor.getCount(), is(INSERTS_COUNT - THREADS_COUNT));
    }

    @Test
    public void testMultithreadingUpdate() throws InterruptedException {
        fillDatabase(YDSContext.APP);
        fillDatabase(YDSContext.USER);

        lock = new CountDownLatch(THREADS_COUNT * 2 + 2);

        new Thread(new WriteRunnable(YDSContext.APP)).start();

        new Thread(new WriteRunnable(YDSContext.USER)).start();

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new UpdateRunnable(YDSContext.APP, i + 1)).start();
        }

        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(new UpdateRunnable(YDSContext.USER, i + 1)).start();
        }

        lock.await(10, TimeUnit.SECONDS);

        final Cursor appCursor =
                databaseManager.openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(appCursor.getCount(), is(INSERTS_COUNT + 1));

        final Cursor userCursor =
                databaseManager.openDatabaseWrapped(YDSContext.USER, MOCK_DATABASE_ID)
                        .query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

        assertThat(userCursor.getCount(), is(INSERTS_COUNT + 1));
    }

    @Test(expected = IllegalStateException.class)
    public void testInsertTransactionCheck() throws InterruptedException {
        final ContentValues values = new ContentValues();
        values.put(INTERNAL_FIELD_ID, MOCK_FIELD_ID);
        values.put(TYPE, MOCK_DATA_TYPE);
        values.put(VALUE, MOCK_DATA_VALUE);

        databaseManager
                .openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                .insert(TABLE_NAME, values);
    }

    @Test
    public void testUpdateTransactionCheck() throws InterruptedException {
        fillDatabase(YDSContext.APP);

        final ContentValues values = new ContentValues();
        values.put(INTERNAL_FIELD_ID, MOCK_FIELD_ID);
        values.put(TYPE, MOCK_DATA_TYPE);
        values.put(VALUE, MOCK_DATA_VALUE);

        try {
            databaseManager
                    .openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                    .update(TABLE_NAME, values, null, null);
        } catch (final IllegalStateException exception) {
            fail("IllegalStateException is not expected");
        }
    }

    @Test
    public void testDeleteTransactionCheck() throws InterruptedException {

        try {
            databaseManager
                    .openDatabaseWrapped(YDSContext.APP, MOCK_DATABASE_ID)
                    .delete(TABLE_NAME, null, null);
        } catch (final IllegalStateException exception) {
            fail("IllegalStateException is not expected");
        }
    }

    private void fillDatabase(@NonNull final YDSContext databaseContext) {
        final SQLiteDatabaseWrapper database =
                databaseManager.openDatabaseWrapped(databaseContext, MOCK_DATABASE_ID);
        database.beginTransaction();

        final ContentValues values = new ContentValues();
        values.put(INTERNAL_FIELD_ID, MOCK_FIELD_ID);
        values.put(TYPE, MOCK_DATA_TYPE);
        values.put(VALUE, MOCK_DATA_VALUE);
        database.insert(TABLE_NAME, values);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    private class WriteRunnable implements Runnable {

        private final YDSContext context;

        public WriteRunnable(@NonNull final YDSContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            final SQLiteDatabaseWrapper database =
                    databaseManager.openDatabaseWrapped(context, MOCK_DATABASE_ID);

            database.beginTransaction();

            for (int i = 0; i < INSERTS_COUNT; i++) {
                final ContentValues values = new ContentValues();
                values.put(INTERNAL_FIELD_ID, MOCK_FIELD_ID);
                values.put(TYPE, MOCK_DATA_TYPE);
                values.put(VALUE, MOCK_DATA_VALUE);
                database.insert(TABLE_NAME, values);
            }
            database.setTransactionSuccessful();
            database.endTransaction();

            lock.countDown();
        }
    }


    private class UpdateRunnable implements Runnable {

        private final int num;

        private final YDSContext context;

        public UpdateRunnable(@NonNull final YDSContext context, final int num) {
            this.context = context;
            this.num = num;
        }

        @Override
        public void run() {
            final SQLiteDatabaseWrapper database =
                    databaseManager.openDatabaseWrapped(context, MOCK_DATABASE_ID);

            database.beginTransaction();

            final ContentValues values = new ContentValues();
            values.put(INTERNAL_FIELD_ID, MOCK_FIELD_ID);
            values.put(TYPE, MOCK_DATA_TYPE);
            values.put(VALUE, MOCK_DATA_VALUE);

            final String selection = ID + "=?";
            final String selectionArgs[] = asStringArray(num);

            database.update(TABLE_NAME, values, selection, selectionArgs);

            database.setTransactionSuccessful();
            database.endTransaction();

            lock.countDown();
        }
    }


    private class DeleteRunnable implements Runnable {

        private final int num;

        private final YDSContext context;

        public DeleteRunnable(@NonNull final YDSContext context, final int num) {
            this.context = context;
            this.num = num;
        }

        @Override
        public void run() {
            final SQLiteDatabaseWrapper database =
                    databaseManager.openDatabaseWrapped(context, MOCK_DATABASE_ID);

            database.beginTransaction();

            final String selection = ID + "=?";
            final String selectionArgs[] = asStringArray(num);

            database.delete(TABLE_NAME, selection, selectionArgs);

            database.setTransactionSuccessful();
            database.endTransaction();

            lock.countDown();
        }
    }


    private class QueryRunnable implements Runnable {

        private final YDSContext context;

        public QueryRunnable(@NonNull final YDSContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            final SQLiteDatabaseWrapper database =
                    databaseManager.openDatabaseWrapped(context, MOCK_DATABASE_ID);

            final Cursor cursor = database.query(DatabaseDescriptor.Value.TABLE_NAME, null, null);

            if (cursor.moveToFirst()) {
                do {
                    final String fieldId =
                            cursor.getString(cursor.getColumnIndex(INTERNAL_FIELD_ID));
                    final String type = cursor.getString(cursor.getColumnIndex(TYPE));
                    final String value = cursor.getString(cursor.getColumnIndex(VALUE));

                    assertThat(fieldId, is(MOCK_FIELD_ID));
                    assertThat(type, is(MOCK_DATA_TYPE));
                    assertThat(value, is(MOCK_DATA_VALUE));
                } while (cursor.moveToNext());
            }
            lock.countDown();
        }
    }
}