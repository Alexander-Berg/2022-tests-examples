/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network.sync;

import androidx.annotation.NonNull;

import com.yandex.datasync.AutoCreateStrategy;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.NetworkException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.observer.RawDataObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;
import java.util.Set;

import static com.yandex.datasync.asserters.DatabaseDtoAsserter.assertDatabase;
import static com.yandex.datasync.asserters.SnapshotResponseAsserter.assertSnapshot;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getDatabaseInfoFromFile;
import static com.yandex.datasync.internal.operation.network.sync.merge.MergeTestObjectFactory.getSnapshotFromFile;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class FirstIncomingSyncStrategyTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_COLLECTION_ID_1 = "sport";

    private static final String MOCK_COLLECTION_ID_2 = "calendar";

    private DatabaseManager databaseManager;

    @Mock
    private Api api;

    @Mock
    private RawDataObserver observable;

    @Before
    public void setUp() {
        initMocks(this);
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
    }

    @Test(expected = NetworkException.class)
    public void testNetworkError() throws BaseException {
        final Set<String> includedCollections = new HashSet<>();

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenThrow(new NetworkException("network error"));

        final SyncStrategy syncStrategy = new FirstIncomingSyncStrategy(MOCK_CONTEXT,
                                                                        MOCK_DATABASE_ID,
                                                                        api,
                                                                        databaseManager,
                                                                        null,
                                                                        includedCollections,
                                                                        AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        syncStrategy.sync();
    }

    @Test
    public void testSnapshotSync() throws Exception {
        final Set<String> includedCollections = new HashSet<>();

        final DatabaseDto databaseInfo = getDatabaseInfoFromFile("get_database_info.json");
        databaseInfo.setFullSnapshotSynced(true);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(databaseInfo);
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID))
                .thenReturn(getSnapshotFromFile("database_snapshot.json"));

        final SyncStrategy syncStrategy = new FirstIncomingSyncStrategy(MOCK_CONTEXT,
                                                                        MOCK_DATABASE_ID,
                                                                        api,
                                                                        databaseManager,
                                                                        null,
                                                                        includedCollections,
                                                                        AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, databaseInfo);

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsSync() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final String collections = convertCollectionList(includedCollections);

        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(getDatabaseInfoFromFile("get_database_info.json"));
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID, collections))
                .thenReturn(getSnapshotFromFile("database_snapshot.json"));

        final SyncStrategy syncStrategy = new FirstIncomingSyncStrategy(MOCK_CONTEXT,
                                                                        MOCK_DATABASE_ID,
                                                                        api,
                                                                        databaseManager,
                                                                        null,
                                                                        includedCollections,
                                                                        AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, getDatabaseInfoFromFile("get_database_info.json"));

        final InOrder order = inOrder(api);

        order.verify(api).getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true);
        order.verify(api).getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID, collections);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testCollectionsSyncEntity() throws Exception {
        final Set<String> includedCollections = new HashSet<>();
        includedCollections.add(MOCK_COLLECTION_ID_1);
        includedCollections.add(MOCK_COLLECTION_ID_2);

        final String collections = convertCollectionList(includedCollections);
        final SnapshotResponse snapshotResponse = getSnapshotFromFile("database_snapshot.json");
        when(api.getDatabaseInfo(MOCK_CONTEXT, MOCK_DATABASE_ID, true))
                .thenReturn(getDatabaseInfoFromFile("get_database_info.json"));
        when(api.getDatabaseSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID, collections))
                .thenReturn(snapshotResponse);

        final SyncStrategy syncStrategy = new FirstIncomingSyncStrategy(MOCK_CONTEXT,
                                                                        MOCK_DATABASE_ID,
                                                                        api,
                                                                        databaseManager,
                                                                        null,
                                                                        includedCollections,
                                                                        AutoCreateStrategy.AUTO_CREATE_ALL_DATABASES);
        final DatabaseDto actualDatabaseInfo = syncStrategy.sync();
        assertDatabase(actualDatabaseInfo, getDatabaseInfoFromFile("get_database_info.json"));

        final SnapshotResponse actualSnapshot = new SnapshotRepository(databaseManager,
                                                                       MOCK_CONTEXT,
                                                                       MOCK_DATABASE_ID).get();
        assertSnapshot(actualSnapshot, snapshotResponse);
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
}