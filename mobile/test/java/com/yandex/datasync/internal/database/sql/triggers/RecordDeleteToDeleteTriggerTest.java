/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.triggers;

import android.content.ContentValues;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.excpetions.RecordAlreadyExistsException;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.RecordChangeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.yandex.datasync.asserters.RecordDtoAsserter.assertRecordChangeType;

@RunWith(RobolectricTestRunner.class)
public class RecordDeleteToDeleteTriggerTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final int MOCK_REVISION = 10;

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "mock_record_id";

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testInsertToInsert() {
        fillInserted();

        fillInserted();

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.INSERT);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testSetToInsert() {
        fillSet();

        fillInserted();

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testUpdateToInsert() {
        fillUpdate();

        fillInserted();

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.UPDATE);
    }

    @Test
    public void testDeleteToInsert() {
        fillDelete();

        fillInserted();

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.INSERT);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testNullToInsert() {
        fillNull();

        fillInserted();

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);
    }

    private ContentValues fillInserted() {
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Record.Rows.COLLECTION_ID, MOCK_COLLECTION_ID);
        values.put(DatabaseDescriptor.Record.Rows.RECORD_ID, MOCK_RECORD_ID);
        values.put(DatabaseDescriptor.Record.Rows.REVISION, MOCK_REVISION);
        values.put(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE, RecordChangeType.INSERT.name());

        databaseWrapper.insert(DatabaseDescriptor.Record.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();

        return values;
    }

    private void fillSet() {
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Record.Rows.COLLECTION_ID, MOCK_COLLECTION_ID);
        values.put(DatabaseDescriptor.Record.Rows.RECORD_ID, MOCK_RECORD_ID);
        values.put(DatabaseDescriptor.Record.Rows.REVISION, MOCK_REVISION);
        values.put(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE, RecordChangeType.SET.name());

        databaseWrapper.insert(DatabaseDescriptor.Record.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private void fillDelete() {
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Record.Rows.COLLECTION_ID, MOCK_COLLECTION_ID);
        values.put(DatabaseDescriptor.Record.Rows.RECORD_ID, MOCK_RECORD_ID);
        values.put(DatabaseDescriptor.Record.Rows.REVISION, MOCK_REVISION);
        values.put(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE, RecordChangeType.DELETE.name());

        databaseWrapper.insert(DatabaseDescriptor.Record.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private void fillUpdate() {
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Record.Rows.COLLECTION_ID, MOCK_COLLECTION_ID);
        values.put(DatabaseDescriptor.Record.Rows.RECORD_ID, MOCK_RECORD_ID);
        values.put(DatabaseDescriptor.Record.Rows.REVISION, MOCK_REVISION);
        values.put(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE, RecordChangeType.UPDATE.name());

        databaseWrapper.insert(DatabaseDescriptor.Record.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private void fillNull(){
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Record.Rows.COLLECTION_ID, MOCK_COLLECTION_ID);
        values.put(DatabaseDescriptor.Record.Rows.RECORD_ID, MOCK_RECORD_ID);
        values.put(DatabaseDescriptor.Record.Rows.REVISION, MOCK_REVISION);
        values.putNull(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE);

        databaseWrapper.insert(DatabaseDescriptor.Record.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }
}