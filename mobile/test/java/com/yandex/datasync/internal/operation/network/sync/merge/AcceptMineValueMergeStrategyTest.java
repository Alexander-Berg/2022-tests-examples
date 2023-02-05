/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network.sync.merge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.editor.SnapshotEditor;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.ChangesRepository;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.LocalChangesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.model.request.ChangesRequest;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.wrappedModels.Snapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.yandex.datasync.asserters.ChangesRequestAsserter.assertChangesRequest;
import static com.yandex.datasync.asserters.SnapshotAsserter.assertSnapshotIgnoreRevision;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getApplyChangesRequest;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getChangesFromFile;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getDatabaseInfoFromFile;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getDeltasFromFile;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getSnapshotFromFile;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class AcceptMineValueMergeStrategyTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_DATABASE_ID_2 = "user_schedule_2";

    private static final long MOCK_REVISION = 1;

    private static final long MOCK_REVISION_2 = 2;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String RECORD_ID = "monday";

    private static final String COLLECTION_ID_1 = "sport";

    private static final String COLLECTION_ID_2 = "meetings";

    private static final String NEW_COLLECTION_ID_1 = "new_sport";

    private static final String NEW_COLLECTION_ID_2 = "new_meetings";

    private static final String FIELD_ID_1 = "starts";

    private static final String FIELD_ID_2 = "ends";

    private static final OperationProcessor processor = new ImmediatelyOperationProcessor();

    public static final String MOCK_STRING_VALUE = "mock_string_value";

    public static final String JSON_MOCK_STRING_VALUE = "json_mock_string_value";

    public static final String MOCK_FIELD_ID = "mock_field_id";

    public static final String MOCK_RECORD_ID = "mock_record_id";

    public static final String MOCK_COLLECTION_ID = "mock_collection_id";

    @Mock
    private Api api;

    private DatabaseManager databaseManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             anyLong(),
                             any()))
                .thenReturn(getApplyChangesRequest(MOCK_REVISION_2));
    }

    @Test
    public void test_WithoutNewCollection_WithoutChanges_WithoutSnapshot_WithoutLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        final SnapshotResponse newCollectionsResponse = null;
        final SnapshotResponse snapshotResponse = null;
        final DeltasResponse deltasResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        verifyNoMoreInteractions(api);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithoutNewCollection_WithChanges_WithoutSnapshot_WithoutLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = getSnaphsotResponse();

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        verifyNoMoreInteractions(api);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithoutNewCollection_WithoutChanges_WithSnapshot_WithoutLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        final SnapshotResponse newCollectionsResponse = null;
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = getSnaphsotResponse();

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        verifyNoMoreInteractions(api);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithoutNewCollection_WithoutChanges_WithoutSnapshot_WithLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        final ArgumentCaptor<ChangesRequest> argument =
                ArgumentCaptor.forClass(ChangesRequest.class);

        verify(api).postChanges(eq(MOCK_CONTEXT),
                                eq(MOCK_DATABASE_ID),
                                eq(newCollectionsResponse.getRevision()),
                                argument.capture());

        final ChangesRequest expectedChangeRequest =
                getChangesFromFile("merge/MineValue/expected_changes.json");

        final ChangesRequest actualChangeRequest = argument.getValue();

        assertChangesRequest(actualChangeRequest, expectedChangeRequest);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        applyLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, actualChangeRequest);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithoutNewCollection_WithChanges_WithoutSnapshot_WithLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotResponse newCollectionsResponse = null;
        final DeltasResponse deltasResponse = getDeltasResponse();
        final SnapshotResponse snapshotResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        final ArgumentCaptor<ChangesRequest> argument =
                ArgumentCaptor.forClass(ChangesRequest.class);

        verify(api).postChanges(eq(MOCK_CONTEXT),
                                eq(MOCK_DATABASE_ID),
                                eq(deltasResponse.getRevision()),
                                argument.capture());

        final ChangesRequest expectedChangeRequest =
                getChangesFromFile("merge/MineValue/expected_changes.json");

        final ChangesRequest actualChangeRequest = argument.getValue();

        assertChangesRequest(actualChangeRequest, expectedChangeRequest);

        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);
        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        applyLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, actualChangeRequest);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithoutNewCollection_WithoutChanges_WithSnapshot_WithLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotResponse newCollectionsResponse = null;
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = getSnaphsotResponse();

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        final ArgumentCaptor<ChangesRequest> argument =
                ArgumentCaptor.forClass(ChangesRequest.class);

        verify(api).postChanges(eq(MOCK_CONTEXT),
                                eq(MOCK_DATABASE_ID),
                                eq(snapshotResponse.getRevision()),
                                argument.capture());

        final ChangesRequest expectedChangeRequest =
                getChangesFromFile("merge/MineValue/expected_changes.json");

        final ChangesRequest actualChangeRequest = argument.getValue();

        assertChangesRequest(actualChangeRequest, expectedChangeRequest);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        applyLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, actualChangeRequest);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithNewCollection_WithoutChanges_WithoutSnapshot_WithoutLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithNewCollection_WithChanges_WithoutSnapshot_WithoutLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = getDeltasResponse();
        final SnapshotResponse snapshotResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        verifyNoMoreInteractions(api);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithNewCollection_WithoutChanges_WithSnapshot_WithoutLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = getSnaphsotResponse();

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        verifyNoMoreInteractions(api);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithNewCollection_WithoutChanges_WithoutSnapshot_WithLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        final ArgumentCaptor<ChangesRequest> argument =
                ArgumentCaptor.forClass(ChangesRequest.class);

        verify(api).postChanges(eq(MOCK_CONTEXT),
                                eq(MOCK_DATABASE_ID),
                                eq(newCollectionsResponse.getRevision()),
                                argument.capture());

        final ChangesRequest expectedChangeRequest =
                getChangesFromFile("merge/MineValue/expected_changes.json");

        final ChangesRequest actualChangeRequest = argument.getValue();

        assertChangesRequest(actualChangeRequest, expectedChangeRequest);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        applyLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, actualChangeRequest);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithNewCollection_WithChanges_WithoutSnapshot_WithLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = getDeltasResponse();
        final SnapshotResponse snapshotResponse = null;

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        final ArgumentCaptor<ChangesRequest> argument =
                ArgumentCaptor.forClass(ChangesRequest.class);

        verify(api).postChanges(eq(MOCK_CONTEXT),
                                eq(MOCK_DATABASE_ID),
                                eq(deltasResponse.getRevision()),
                                argument.capture());

        final ChangesRequest expectedChangeRequest =
                getChangesFromFile("merge/MineValue/expected_changes.json");

        final ChangesRequest actualChangeRequest = argument.getValue();

        assertChangesRequest(actualChangeRequest, expectedChangeRequest);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        applyLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, actualChangeRequest);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    @Test
    public void test_WithNewCollection_WithoutChanges_WithSnapshot_WithLocalChanges()
            throws Exception {
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2);

        addChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final SnapshotResponse newCollectionsResponse = getNewCollectionsResponse();
        final DeltasResponse deltasResponse = null;
        final SnapshotResponse snapshotResponse = getSnaphsotResponse();

        final MergeStrategy mergeStrategy = new AcceptMineValueMergeStrategy(MOCK_CONTEXT,
                                                                             MOCK_DATABASE_ID,
                                                                             MOCK_REVISION,
                                                                             databaseManager,
                                                                             api,
                                                                             newCollectionsResponse,
                                                                             deltasResponse,
                                                                             snapshotResponse,
                                                                             true);
        mergeStrategy.merge();

        final ArgumentCaptor<ChangesRequest> argument =
                ArgumentCaptor.forClass(ChangesRequest.class);

        verify(api).postChanges(eq(MOCK_CONTEXT),
                                eq(MOCK_DATABASE_ID),
                                eq(snapshotResponse.getRevision()),
                                argument.capture());

        final ChangesRequest expectedChangeRequest =
                getChangesFromFile("merge/MineValue/expected_changes.json");

        final ChangesRequest actualChangeRequest = argument.getValue();

        assertChangesRequest(actualChangeRequest, expectedChangeRequest);

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, deltasResponse);
        applyLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, actualChangeRequest);
        fillNewCollections(MOCK_CONTEXT, MOCK_DATABASE_ID_2, newCollectionsResponse);

        final Snapshot actualSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshotResponse = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshotResponse, expectedSnapshotResponse);
    }

    private void fillNewCollections(@NonNull final YDSContext context,
                                    @NonNull final String databaseId,
                                    @Nullable final SnapshotResponse newCollectionsResponse)
            throws IOException {
        if (newCollectionsResponse != null) {
            new SnapshotRepository(databaseManager, context, databaseId).save(
                    newCollectionsResponse);
        }
    }

    private void fillDatabase(@NonNull final YDSContext context,
                              @NonNull final String databaseId) throws IOException {
        final SnapshotResponse snapshotResponse =
                getSnapshotFromFile("merge/database_snapshot_1.json");
        snapshotResponse.setDatabaseId(databaseId);
        new SnapshotRepository(databaseManager, context, databaseId).save(snapshotResponse);

        final DatabaseDto databaseInfo = getDatabaseInfoFromFile("merge/get_database_info.json");
        databaseInfo.setDatabaseId(databaseId);
        new DatabasesRepository(databaseManager, context).save(databaseInfo);
    }

    private void addChanges(@NonNull final YDSContext databaseContext,
                            @NonNull final String databaseId) throws BaseException {

        final SnapshotEditor snapshotEditor = getSnapshot(databaseContext, databaseId).edit();
        addNewCollection(snapshotEditor);
        editValues(snapshotEditor);
        snapshotEditor.commit();
    }

    private void addNewCollection(@NonNull final SnapshotEditor snapshotEditor) {
        snapshotEditor.addCollection(MOCK_COLLECTION_ID)
                .addRecord(MOCK_RECORD_ID)
                .addField(MOCK_FIELD_ID)
                .putStringValue(MOCK_STRING_VALUE);
    }

    private void editValues(@NonNull final SnapshotEditor snapshotEditor) {
        snapshotEditor.editCollection(COLLECTION_ID_1)
                .editRecord(RECORD_ID)
                .editField(FIELD_ID_1)
                .putStringValue(MOCK_STRING_VALUE);

        snapshotEditor.editCollection(COLLECTION_ID_2)
                .editRecord(RECORD_ID)
                .editField(FIELD_ID_2)
                .putStringValue(MOCK_STRING_VALUE);
    }

    private SnapshotResponse getNewCollectionsResponse() throws IOException {
        return getSnapshotFromFile("merge/database_snapshot_1_new_collections.json");
    }

    private SnapshotResponse getSnaphsotResponse() throws IOException {
        return getSnapshotFromFile("merge/database_snapshot_1.json");
    }

    private DeltasResponse getDeltasResponse() throws IOException {
        return getDeltasFromFile("merge/deltas.json");
    }

    private Snapshot getSnapshot(@NonNull final YDSContext databaseContext,
                                 @NonNull final String databaseId) throws BaseException {
        final SnapshotResponse snapshotResponse = getSnapshotResponse(databaseContext,
                                                                      databaseId);
        return new Snapshot(databaseManager,
                            databaseContext,
                            databaseId,
                            processor,
                            snapshotResponse);
    }

    private SnapshotResponse getSnapshotResponse(@NonNull final YDSContext databaseContext,
                                                 @NonNull final String databaseId)
            throws BaseException {
        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     databaseContext,
                                                                     databaseId);
        return repository.get();
    }

    private void applyLocalChanges(@NonNull final YDSContext databaseContext,
                                   @NonNull final String databaseId,
                                   @NonNull final ChangesRequest changesRequest) {
        new LocalChangesRepository(databaseManager, databaseContext, databaseId)
                .save(changesRequest.getChanges());
    }

    private void applyChanges(@NonNull final YDSContext databaseContext,
                              @NonNull final String databaseId,
                              @Nullable final DeltasResponse deltasResponse) throws IOException {
        if (deltasResponse != null) {
            new ChangesRepository(databaseManager, databaseContext, databaseId, true)
                    .save(deltasResponse);
        }
    }
}
