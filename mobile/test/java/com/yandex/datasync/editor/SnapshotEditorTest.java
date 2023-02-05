/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.editor;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.RecordChangeType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SnapshotEditorTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "mock_record_id";

    @Mock
    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testEditCollectionEditRecord() throws Exception {
        final SnapshotEditor snapshotEditor = new SnapshotEditor(databaseManager,
                                                                 MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID);

        final CollectionEditor collectionEditor = snapshotEditor.editCollection(MOCK_COLLECTION_ID);
        collectionEditor.editRecord(MOCK_RECORD_ID);

        final List<ChangesDto> changesList = snapshotEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getChangeType(), is(RecordChangeType.UPDATE));

        final List<ChangeDto> changeList = changesDto.getChangeList();
        assertThat(changeList.size(), is(0));
    }

    @Test
    public void testEditCollectionAddRecord() throws Exception {
        final SnapshotEditor snapshotEditor = new SnapshotEditor(databaseManager,
                                                                 MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID);

        final CollectionEditor collectionEditor = snapshotEditor.editCollection(MOCK_COLLECTION_ID);
        collectionEditor.addRecord(MOCK_RECORD_ID);

        final List<ChangesDto> changesList = snapshotEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getChangeType(), is(RecordChangeType.INSERT));

        final List<ChangeDto> changeList = changesDto.getChangeList();
        assertThat(changeList.size(), is(0));
    }

    @Test
    public void testEditCollectionRemoveRecord() throws Exception {
        final SnapshotEditor snapshotEditor = new SnapshotEditor(databaseManager,
                                                                 MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID);

        final CollectionEditor collectionEditor = snapshotEditor.editCollection(MOCK_COLLECTION_ID);
        collectionEditor.removeRecord(MOCK_RECORD_ID);

        final List<ChangesDto> changesList = snapshotEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getChangeType(), is(RecordChangeType.DELETE));

        final List<ChangeDto> changeList = changesDto.getChangeList();
        assertThat(changeList.size(), is(0));
    }

    @Test
    public void testAddCollection() throws Exception {
        final SnapshotEditor snapshotEditor = new SnapshotEditor(databaseManager,
                                                                 MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID);

        final CollectionEditor collectionEditor = snapshotEditor.addCollection(MOCK_COLLECTION_ID);
        collectionEditor.addRecord(MOCK_RECORD_ID);

        final List<ChangesDto> changesList = snapshotEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getChangeType(), is(RecordChangeType.INSERT));

        final List<ChangeDto> changeList = changesDto.getChangeList();
        assertThat(changeList.size(), is(0));
    }

    @Test
    public void testAddCollectionRemoveCollection() throws Exception {
        final SnapshotEditor snapshotEditor = new SnapshotEditor(databaseManager,
                                                                 MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID);

        final CollectionEditor collectionEditor1 = snapshotEditor.addCollection(MOCK_COLLECTION_ID);
        collectionEditor1.editRecord(MOCK_RECORD_ID);

        final CollectionEditor collectionEditor2 = snapshotEditor.addCollection(MOCK_COLLECTION_ID);
        collectionEditor2.removeRecord(MOCK_RECORD_ID);

        final List<ChangesDto> changesList = snapshotEditor.build();
        assertThat(changesList.size(), is(0));
    }

    @Test
    public void testAddCollectionWithoutRecord() throws Exception {
        final SnapshotEditor snapshotEditor = new SnapshotEditor(databaseManager,
                                                                 MOCK_CONTEXT,
                                                                 MOCK_DATABASE_ID);

        snapshotEditor.addCollection(MOCK_COLLECTION_ID);

        final List<ChangesDto> changesList = snapshotEditor.build();
        assertTrue(changesList.isEmpty());
    }
}