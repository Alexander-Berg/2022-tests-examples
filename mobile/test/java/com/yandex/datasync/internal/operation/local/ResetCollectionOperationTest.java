/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.local;

import android.database.Cursor;
import androidx.annotation.NonNull;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.operation.Operation;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Record.Rows.COLLECTION_ID;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Record.TABLE_NAME;
import static com.yandex.datasync.internal.database.sql.triggers.TriggersTestObjectFactory.fillDatabase;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ResetCollectionOperationTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_COLLECTION_ID = "calendar";

    private static final String MOCK_COLLECTION_ID_2 = "sport";

    private DatabaseManager databaseManager;

    private SQLiteDatabaseWrapper databaseWrapper;

    private MockRawDataObserver mockRawDataObserver;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
        mockRawDataObserver = new MockRawDataObserver();
    }

    @Test
    public void testSnapshot() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final String selection = COLLECTION_ID + "=?";
        String selectionArgs[] = {MOCK_COLLECTION_ID};

        try (final Cursor cursor = databaseWrapper.query(TABLE_NAME, selection, selectionArgs)) {
            assertTrue(cursor.moveToFirst());
        }

        selectionArgs = new String[]{MOCK_COLLECTION_ID_2};

        try (final Cursor cursor = databaseWrapper.query(TABLE_NAME, selection, selectionArgs)) {
            assertTrue(cursor.moveToFirst());
        }
    }

    @Test
    public void testResetCollection() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        resetCollection(MOCK_COLLECTION_ID);

        final String selection = COLLECTION_ID + "=?";
        String selectionArgs[] = {MOCK_COLLECTION_ID};

        try (final Cursor cursor = databaseWrapper.query(TABLE_NAME, selection, selectionArgs)) {
            assertFalse(cursor.moveToFirst());
        }

        selectionArgs = new String[]{MOCK_COLLECTION_ID_2};

        try (final Cursor cursor = databaseWrapper.query(TABLE_NAME, selection, selectionArgs)) {
            assertTrue(cursor.moveToFirst());
        }
    }

    @Test
    public void testObserver() throws IOException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        resetCollection(MOCK_COLLECTION_ID);

        assertNotNull(mockRawDataObserver.getDatabaseContext());
        assertNotNull(mockRawDataObserver.getDatabaseId());
        assertNotNull(mockRawDataObserver.getCollectionId());

        assertThat(MOCK_CONTEXT, is(mockRawDataObserver.getDatabaseContext()));
        assertThat(MOCK_DATABASE_ID, is(mockRawDataObserver.getDatabaseId()));
        assertThat(MOCK_COLLECTION_ID, is(mockRawDataObserver.getCollectionId()));
    }

    private void resetCollection(@NonNull final String collectionId) {
        final Operation operation = new ResetCollectionOperation(MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID,
                                                                 collectionId,
                                                                 databaseManager,
                                                                 mockRawDataObserver);
        operation.run();
    }
}