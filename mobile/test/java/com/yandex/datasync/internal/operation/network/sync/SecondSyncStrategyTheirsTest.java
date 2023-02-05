/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network.sync;

import android.content.ContentValues;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.datasync.AutoCreateStrategy;
import com.yandex.datasync.MergeAtomSize;
import com.yandex.datasync.MergeWinner;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.editor.RecordEditor;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.NetworkException;
import com.yandex.datasync.internal.api.exceptions.http.GoneException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.DatabaseDescriptor;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.DatabaseChangeType;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.ErrorResponse;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.RecordsDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.internal.util.DateFormat;
import com.yandex.datasync.wrappedModels.Snapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yandex.datasync.asserters.DatabaseDtoAsserter.assertDatabase;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getApplyChangesRequest;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getDatabaseInfoFromFile;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getDeltasFromFile;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getSnapshotFromFile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class SecondSyncStrategyTheirsTest {

    @ParameterizedRobolectricTestRunner.Parameters(name = "MergeWinner = {0}, MergeAtomSize = {1}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{

                {MergeWinner.THEIRS.name(), MergeAtomSize.COLLECTION.name()},
                {MergeWinner.THEIRS.name(), MergeAtomSize.RECORD.name()},
                {MergeWinner.THEIRS.name(), MergeAtomSize.VALUE.name()},

                {MergeWinner.MINE.name(), MergeAtomSize.COLLECTION.name()},
                {MergeWinner.MINE.name(), MergeAtomSize.RECORD.name()},
                {MergeWinner.MINE.name(), MergeAtomSize.VALUE.name()}
        });
    }

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_COLLECTION_ID = "sport";

    private static final String MOCK_NEW_COLLECTION_ID = "new_sport";

    private static final String MOCK_COLLECTION_ID_1 = "mock_collection_id_1";

    private static final String MOCK_COLLECTION_ID_2 = "mock_collection_id_2";

    private static final String MOCK_RECORD_ID = "monday";

    private static final String MOCK_FILED_ID_1 = "starts";

    private static final String MOCK_FIELD_ID_2 = "ends";

    private static final String MOCK_FIELD_1_VALUE = "10 p.m.";

    private static final String MOCK_FIELD_2_VALUE = "12 p.m.";

    private final OperationProcessor processor = new ImmediatelyOperationProcessor();

    private DatabaseManager databaseManager;

    @Mock
    private Api api;

    private final MergeAtomSize mergeAtomSize;

    private final MergeWinner mergeWinner;

    public SecondSyncStrategyTheirsTest(@NonNull final String mergeWinnerName,
                                        @NonNull final String mergeAtomSizeName) {
        this.mergeWinner = MergeWinner.valueOf(mergeWinnerName);
        this.mergeAtomSize = MergeAtomSize.valueOf(mergeAtomSizeName);
    }

    @Before
    public void setUp() {
        initMocks(this);
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test(expected = NetworkException.class)
    public void testNetworkErrorOnDatabaseInfo() throws Exception {
        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenThrow(new NetworkException("network error"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(new HashSet<>(), localDatabaseInfo);
        syncStrategy.sync();
    }

    @Test(expected = NetworkException.class)
    public void testNetworkErrorOnGetDeltas() throws Exception {
        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(getDatabaseInfoFromFile("get_database_info_2.json"));

        when(api.getDeltas(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), anyLong()))
                .thenThrow(new NetworkException("network error"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(new HashSet<>(), localDatabaseInfo);
        syncStrategy.sync();
    }

    @Test(expected = NetworkException.class)
    public void testNetworkErrorGoneOnGetSnapshot() throws Exception {
        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(getDatabaseInfoFromFile("get_database_info_2.json"));

        when(api.getDeltas(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), anyLong()))
                .thenThrow(new GoneException(new ErrorResponse()));

        when(api.getDatabaseSnapshot(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID)))
                .thenThrow(new NetworkException("network error"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(new HashSet<>(), localDatabaseInfo);
        syncStrategy.sync();
    }

    @Test
    public void testFullSnapshotWithoutLocalChangesWithoutNewCollections() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add("sport");

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(true);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(getDeltasFromFile("changes/update_record_set_field.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api)
                .getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncFullSnapshotWithoutLocalChangesWithNewCollections() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(true);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(getSnapshotFromFile("database_snapshot.json"));
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(getDeltasFromFile("changes/update_record_set_field.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testFullSnapshotWithLocalChangesWithoutNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(localDatabaseInfo.isFullSnapshotSynced());

        final DeltasResponse deltasResponse =
                getDeltasFromFile("changes/update_record_set_field.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(deltasResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(deltasResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(deltasResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncFullSnapshotWithLocalChangesWithNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(localDatabaseInfo.isFullSnapshotSynced());

        final DeltasResponse deltasResponse =
                getDeltasFromFile("changes/update_record_set_field.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(deltasResponse);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(getSnapshotFromFile("database_snapshot.json"));
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(deltasResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(deltasResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testFullSnapshotWithoutLocalChangesGoneWithoutNewCollections()
            throws Exception {

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(true);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(getSnapshotFromFile("database_snapshot_2.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api)
                .getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testFullSnapshotWithoutLocalChangesGoneWithNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot_2.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(snapshotResponse);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(snapshotResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshotResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);

        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshotResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testFullSnapshotWithLocalChangesGoneWithoutNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot_2.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(snapshotResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshotResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);

        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshotResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSyncFullSnapshotWithLocalChangesGoneWithNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot_2.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(snapshotResponse);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(snapshotResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshotResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);

        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshotResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithoutLocalChangesWithoutNewCollections() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add("sport");

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(false);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(getDeltasFromFile("changes/update_record_set_field.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithoutLocalChangesWithNewCollections() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(false);

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(snapshotResponse);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(getDeltasFromFile("changes/update_record_set_field.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithLocalChangesWithoutNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        final DeltasResponse deltasResponse =
                getDeltasFromFile("changes/update_record_set_field.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(deltasResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(deltasResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(deltasResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithLocalChangesWithNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot_2.json");

        final DeltasResponse deltasResponse =
                getDeltasFromFile("changes/update_record_set_field.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(snapshotResponse);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenReturn(deltasResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(deltasResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(deltasResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithoutLocalChangesGoneWithoutNewCollections() throws Exception {
        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(false);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), any()))
                .thenReturn(getSnapshotFromFile("database_snapshot.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final String expectedCollectionsIds = getExpectedCollectionsIds(includedCollections,
                                                                        null);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(eq(MOCK_CONTEXT),
                                              eq(MOCK_DATABASE_ID),
                                              eq(expectedCollectionsIds));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithoutLocalChangesGoneWithNewCollections() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        localDatabaseInfo.setFullSnapshotSynced(false);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), any()))
                .thenReturn(getSnapshotFromFile("database_snapshot.json"));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final String expectedCollectionsIds = getExpectedCollectionsIds(includedCollections,
                                                                        null);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(eq(MOCK_CONTEXT),
                                              eq(MOCK_DATABASE_ID),
                                              eq(expectedCollectionsIds));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithLocalChangesGoneWithoutNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localDatabaseInfo.setFullSnapshotSynced(false);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);
        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(eq(MOCK_CONTEXT), eq(MOCK_DATABASE_ID), any()))
                .thenReturn(snapshotResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshotResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   snapshotResponse.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshotResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsWithLocalChangesGoneWithNewCollections() throws Exception {
        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localDatabaseInfo.setFullSnapshotSynced(false);
        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info_2.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);
        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDeltas(MOCK_CONTEXT, MOCK_DATABASE_ID, localDatabaseInfo.getRevision()))
                .thenThrow(new GoneException(new ErrorResponse()));
        when(api.getDatabaseSnapshot(eq(MOCK_CONTEXT),
                                     eq(MOCK_DATABASE_ID),
                                     not(eq(convertCollectionList(includedCollections)))))
                .thenReturn(snapshotResponse);

        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(getSnapshotFromFile("apply_changes/snapshot_step_1.json"));

        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshotResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   snapshotResponse.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final String expectedCollectionsIds = getExpectedCollectionsIds(includedCollections,
                                                                        snapshotResponse);
        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).getDeltas(MOCK_CONTEXT,
                                    MOCK_DATABASE_ID,
                                    localDatabaseInfo.getRevision());
        order.verify(api).getDatabaseSnapshot(eq(MOCK_CONTEXT),
                                              eq(MOCK_DATABASE_ID),
                                              anyString());
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshotResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsOutgoingSyncOnlyWithoutNewCollections() throws Exception {

        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false, 1);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localDatabaseInfo.setFullSnapshotSynced(false);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(serverDatabaseInfo.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);

        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(serverDatabaseInfo.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsOutgoingSyncOnlyWithNewCollections() throws Exception {

        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false, 1);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        serverDatabaseInfo.setFullSnapshotSynced(false);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localDatabaseInfo.setFullSnapshotSynced(false);

        final SnapshotResponse snapshotResponse =
                getSnapshotFromFile("apply_changes/snapshot_step_1.json");

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT,
                                     MOCK_DATABASE_ID,
                                     convertCollectionList(includedCollections)))
                .thenReturn(snapshotResponse);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(snapshotResponse.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);

        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              convertCollectionList(includedCollections));
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(snapshotResponse.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testOutgoingSyncOnly() throws Exception {

        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false, 1);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);
        addNewCollectionChangesLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localDatabaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(serverDatabaseInfo.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   serverDatabaseInfo.getRevision()));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, serverDatabaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(serverDatabaseInfo.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    @Test
    public void localDatabaseRevisionMustBeUpdatedAfterChangesPush() throws Exception {

        fillLocalDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, false, 1);
        fillLocalDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        addIntersectedLocalChanges(MOCK_CONTEXT, MOCK_DATABASE_ID);

        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto serverDatabaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        serverDatabaseInfo.setFullSnapshotSynced(true);

        final DatabaseDto localDatabaseInfo = getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localDatabaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(serverDatabaseInfo);

        final long newRevision = localDatabaseInfo.getRevision() + 1;
        when(api.postChanges(eq(MOCK_CONTEXT),
                             eq(MOCK_DATABASE_ID),
                             eq(serverDatabaseInfo.getRevision()),
                             any()))
                .thenReturn(getApplyChangesRequest("post_changes.json",
                                                   newRevision));

        final SyncStrategy syncStrategy =
                createSecondSyncStrategy(includedCollections, localDatabaseInfo);

        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();

        assertThat(actualDatabaseInfo.getRevision(), equalTo(newRevision));

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).postChanges(eq(MOCK_CONTEXT),
                                      eq(MOCK_DATABASE_ID),
                                      eq(localDatabaseInfo.getRevision()),
                                      any());
        order.verifyNoMoreInteractions();
    }

    private SecondSyncStrategy createSecondSyncStrategy(final Set<String> includedCollections,
                                                        final DatabaseDto localDatabaseInfo) {
        return new SecondSyncStrategy(mergeWinner,
                                      mergeAtomSize,
                                      MOCK_CONTEXT,
                                      MOCK_DATABASE_ID,
                                      api,
                                      null,
                                      localDatabaseInfo,
                                      databaseManager,
                                      includedCollections,
                                      AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES
        );
    }

    private String getExpectedCollectionsIds(@NonNull final Set<String> includedCollections,
                                             @Nullable final SnapshotResponse snapshotResponse) {
        final Set<String> result = getSnapshotCollectionsIds(snapshotResponse);
        return convertCollectionList(result);
    }

    private String convertCollectionList(@NonNull final Set<String> collectionSet) {
        final StringBuilder collectionIdsBuilder = new StringBuilder();

        for (final String collectionId : collectionSet) {
            if (collectionIdsBuilder.length() > 0) {
                collectionIdsBuilder.append(',');
            }
            collectionIdsBuilder.append(collectionId);
        }

        return collectionIdsBuilder.toString();
    }

    private void fillLocalDatabaseInfo(@NonNull final YDSContext databaseContext,
                                       @NonNull final String databaseId,
                                       final boolean fullSnapshotSynced) {
        fillLocalDatabaseInfo(databaseContext, databaseId, fullSnapshotSynced, 0);
    }

    private void fillLocalDatabaseInfo(@NonNull final YDSContext databaseContext,
                                       @NonNull final String databaseId,
                                       final boolean fullSnapshotSynced,
                                       final long revision) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext);
        databaseWrapper.beginTransaction();

        final ContentValues values = new ContentValues();

        final String currentDate = DateFormat.format(new Date());

        values.put(DatabaseDescriptor.Databases.Rows.CREATED, currentDate);
        values.put(DatabaseDescriptor.Databases.Rows.MODIFIED, currentDate);
        values.put(DatabaseDescriptor.Databases.Rows.FULL_SNAPSHOT_SYNCED, fullSnapshotSynced);
        values.put(DatabaseDescriptor.Databases.Rows.RECORDS_COUNT, 0);
        values.put(DatabaseDescriptor.Databases.Rows.REVISION, revision);
        values.put(DatabaseDescriptor.Databases.Rows.SYNCED, DatabaseChangeType.INSERT.name());
        values.put(DatabaseDescriptor.Databases.Rows.DATABASE_ID, databaseId);

        databaseWrapper.insert(DatabaseDescriptor.Databases.TABLE_NAME, values);
        databaseWrapper.setTransactionSuccessful();
        databaseWrapper.endTransaction();
    }

    private void fillLocalDatabase(@NonNull final YDSContext databaseContext,
                                   @NonNull final String databaseId,
                                   @NonNull final String fileName) throws IOException {
        final SnapshotResponse snapshotResponse = getSnapshotFromFile(fileName);
        new SnapshotRepository(databaseManager, databaseContext, databaseId).save(snapshotResponse);
    }

    private void addIntersectedLocalChanges(@NonNull final YDSContext databaseContext,
                                            @NonNull final String databaseId)
            throws BaseException {
        final Snapshot snapshot = getSnapshot(databaseContext, databaseId);
        final RecordEditor recordEditor =
                snapshot.edit()
                        .editCollection(MOCK_COLLECTION_ID)
                        .editRecord(MOCK_RECORD_ID);

        recordEditor.addField(MOCK_FILED_ID_1).putStringValue(MOCK_FIELD_1_VALUE);
        recordEditor.addField(MOCK_FIELD_ID_2).putStringValue(MOCK_FIELD_2_VALUE);
        recordEditor.commit();
    }

    private void addNewCollectionChangesLocalChanges(@NonNull final YDSContext databaseContext,
                                                     @NonNull final String databaseId)
            throws BaseException {
        final Snapshot snapshot = getSnapshot(databaseContext, databaseId);
        final RecordEditor recordEditor =
                snapshot.edit()
                        .addCollection(MOCK_NEW_COLLECTION_ID)
                        .addRecord(MOCK_RECORD_ID);

        recordEditor.addField(MOCK_FILED_ID_1).putStringValue(MOCK_FIELD_1_VALUE);
        recordEditor.addField(MOCK_FIELD_ID_2).putStringValue(MOCK_FIELD_2_VALUE);
        recordEditor.commit();
    }

    private Snapshot getSnapshot(@NonNull final YDSContext databaseContext,
                                 @NonNull final String databaseId) throws BaseException {
        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     databaseContext,
                                                                     databaseId);
        final SnapshotResponse snapshotResponse = repository.get();
        return new Snapshot(databaseManager,
                            databaseContext,
                            databaseId,
                            processor,
                            snapshotResponse);
    }

    private DatabaseDto getDatabaseInfo(@NonNull final YDSContext databaseContext,
                                        @NonNull final String databaseId) {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(databaseContext);
        return new DatabasesRepository(databaseWrapper).get(databaseId);
    }

    private Set<String> getSnapshotCollectionsIds(@Nullable final SnapshotResponse snapshot) {
        final Set<String> result = new HashSet<>();
        if (snapshot != null) {
            final RecordsDto recordsDto = snapshot.getRecords();
            if (recordsDto != null) {
                for (final RecordDto record : recordsDto.getItems()) {
                    result.add(record.getCollectionId());
                }
            }
        }
        return result;
    }
}
