/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.triggers;

import android.content.ContentValues;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.excpetions.RecordAlreadyExistsException;
import com.yandex.datasync.internal.database.excpetions.RecordNotExistsException;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.RecordChangeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.yandex.datasync.asserters.FieldDtoAsserter.assertFieldsExists;
import static com.yandex.datasync.asserters.FieldDtoAsserter.assertFieldsNotExists;
import static com.yandex.datasync.asserters.RecordDtoAsserter.assertRecordChangeType;
import static com.yandex.datasync.asserters.RecordDtoAsserter.assertRecordNotExists;

@RunWith(RobolectricTestRunner.class)
public class RecordsStateChangeBehaviourTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final int MOCK_REVISION = 10;

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "mock_record_id";

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() {
        final DatabaseManager databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test
    public void testUpdateToDelete() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.DELETE);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testUpdateToInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
    }

    @Test
    public void testUpdateToSetViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testUpdateToSetViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testUpdateToUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.UPDATE);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testUpdateToNullViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testUpdateToNullViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testInsertToDelete() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);

        assertRecordNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testInsertToInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
    }

    @Test
    public void testInsertToSetViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testInsertToSetViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testInsertToUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.INSERT);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testInsertToNullViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testInsertToNullViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testSetToDelete() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.DELETE);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testSetToInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
    }

    @Test
    public void testSetToSetViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testSetToSetViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testSetToUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testSetToNullViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testSetToNullViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testDeleteToDelete() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
    }

    @Test
    public void testDeleteToInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.INSERT);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testDeleteToSetViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testDeleteToSetViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testDeleteToUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
    }

    @Test
    public void testDeleteToNullViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testDeleteToNullViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testNullToDelete() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.DELETE);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testNullToInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
    }

    @Test
    public void testNullToSetViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.SET);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testNullToSetViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
    }

    @Test
    public void testNullToUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               RecordChangeType.UPDATE);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testNullToNullViaUpdate() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test
    public void testNullToNullViaInsert() {
        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);
        addField(MOCK_COLLECTION_ID, MOCK_RECORD_ID, MOCK_FIELD_ID);

        addRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);

        assertFieldsNotExists(databaseWrapper, MOCK_COLLECTION_ID, MOCK_RECORD_ID);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testNotExistsToDelete() {
        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.DELETE);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testNotExistsToInsert() {
        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.INSERT);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testNotExistsToSetViaUpdate() {
        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testNotExistsToSetViaInsert() {
        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.SET);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testNotExistsToUpdate() {
        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, RecordChangeType.UPDATE);
    }

    @Test(expected = RecordNotExistsException.class)
    public void testNotExistsToNullViaUpdate() {
        updateRecord(MOCK_COLLECTION_ID, MOCK_RECORD_ID, null);

        assertRecordChangeType(databaseWrapper,
                               MOCK_COLLECTION_ID,
                               MOCK_RECORD_ID,
                               null);
    }

    private void addRecord(@NonNull final String collectionId,
                           @NonNull final String recordId,
                           @Nullable final RecordChangeType recordChangeType) {
        final ContentValues values = new ContentValues();
        values.put(DatabaseDescriptor.Record.Rows.COLLECTION_ID, collectionId);
        values.put(DatabaseDescriptor.Record.Rows.RECORD_ID, recordId);
        values.put(DatabaseDescriptor.Record.Rows.REVISION, MOCK_REVISION);
        if (recordChangeType == null) {
            values.putNull(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE);
        } else {
            values.put(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE,
                       recordChangeType.name());
        }

        databaseWrapper.beginTransaction();
        databaseWrapper.insert(DatabaseDescriptor.Record.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private void updateRecord(@NonNull final String collectionId,
                              @NonNull final String recordId,
                              @Nullable final RecordChangeType recordChangeType) {
        final ContentValues values = new ContentValues();

        if (recordChangeType == null) {
            values.putNull(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE);
        } else {
            values.put(DatabaseDescriptor.Record.Rows.INTERNAL_CHANGE_TYPE,
                       recordChangeType.name());
        }

        final String selection = DatabaseDescriptor.Record.Rows.COLLECTION_ID + " =? " +
                                 " AND " + DatabaseDescriptor.Record.Rows.RECORD_ID + " =? ";
        final String selectionArgs[] = {collectionId, recordId};

        databaseWrapper.update(DatabaseDescriptor.Record.TABLE_NAME,
                               values,
                               selection,
                               selectionArgs);
    }

    private void addField(@NonNull final String collectionId,
                          @NonNull final String recordId,
                          @NonNull final String fieldId) {

        final ContentValues values = new ContentValues();

        values.put(DatabaseDescriptor.Field.Rows.COLLECTION_ID, collectionId);
        values.put(DatabaseDescriptor.Field.Rows.RECORD_ID, recordId);
        values.put(DatabaseDescriptor.Field.Rows.FIELD_ID, fieldId);

        databaseWrapper.beginTransaction();
        databaseWrapper.insert(DatabaseDescriptor.Field.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }
}
