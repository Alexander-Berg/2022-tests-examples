/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.repository;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Datatype;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.editor.CollectionEditor;
import com.yandex.datasync.editor.FieldEditor;
import com.yandex.datasync.editor.FieldListEditor;
import com.yandex.datasync.editor.RecordEditor;
import com.yandex.datasync.editor.SnapshotEditor;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.excpetions.RecordAlreadyExistsException;
import com.yandex.datasync.internal.database.excpetions.RecordNotExistsException;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.RecordChangeType;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DeltaItemDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.wrappedModels.Snapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.yandex.datasync.asserters.ChangesDtoAsserter.assertFieldChanges;
import static com.yandex.datasync.asserters.ChangesDtoAsserter.assertFieldListChanges;
import static com.yandex.datasync.asserters.ChangesDtoAsserter.assertRecordChanges;
import static com.yandex.datasync.asserters.ChangesDtoAsserter.assertValueChange;
import static com.yandex.datasync.asserters.ChangesListAsserter.assertChangesList;
import static com.yandex.datasync.asserters.SnapshotAsserter.assertSnapshotIgnoreRevision;
import static com.yandex.datasync.util.ResourcesUtil.getTextFromFile;

@RunWith(RobolectricTestRunner.class)
public class LocalChangesRepositoryTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_DATABASE_ID_2 = "user_schedule2";

    private static final String MOCK_USER_ID = "mock_user_id";

    private OperationProcessor processor;

    private DatabaseManager databaseManager;

    private LocalChangesRepository localChangesRepository;

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
        localChangesRepository = new LocalChangesRepository(databaseManager,
                                                            MOCK_CONTEXT,
                                                            MOCK_DATABASE_ID);
    }

    @Test
    public void testInsertRecordSetField() throws Exception {
        fillDatabaseInfo("get_database_info.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.addCollection("sport");
        final RecordEditor recordEditor = collectionEditor.addRecord("meeting");
        final FieldEditor fieldEditor1 = recordEditor.addField("starts");
        fieldEditor1.putStringValue("3 p.m.");
        final FieldEditor fieldEditor2 = recordEditor.addField("finishes");
        fieldEditor2.putIntegerValue(4);

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.INSERT, "sport", "meeting");

        assertFieldChanges(databaseWrapper,
                           FieldChangeType.SET,
                           "sport",
                           "meeting",
                           "starts");

        assertFieldChanges(databaseWrapper,
                           FieldChangeType.SET,
                           "sport",
                           "meeting",
                           "finishes");

        assertValueChange(databaseWrapper,
                          FieldChangeType.SET,
                          "sport",
                          "meeting",
                          "finishes",
                          Datatype.STRING,
                          "3 p.m.");

        assertValueChange(databaseWrapper,
                          FieldChangeType.SET,
                          "sport",
                          "meeting",
                          "finishes",
                          Datatype.STRING,
                          "4");
    }

    @Test
    public void testInsertRecordSetFieldViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.addRecord("meeting");
        final FieldEditor fieldEditor1 = recordEditor.addField("starts");
        fieldEditor1.putStringValue("3 p.m.");
        final FieldEditor fieldEditor2 = recordEditor.addField("finishes");
        fieldEditor2.putIntegerValue(4);

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/insert_record_set_field.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(actualChangesList, expectedChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordDeleteField() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.editRecord("monday");
        recordEditor.removeField("starts");
        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "sport", "monday");
        assertFieldChanges(databaseWrapper, FieldChangeType.DELETE, "sport", "monday", "starts");
    }

    @Test
    public void testUpdateRecordDeleteFieldViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.editRecord("monday");
        recordEditor.removeField("starts");
        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_delete_field.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(expectedChangesList, actualChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordInsertField() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.editRecord("monday");
        final FieldEditor fieldEditor1 = recordEditor.addField("break_start");
        fieldEditor1.putStringValue("3:30 p.m.");
        final FieldEditor fieldEditor2 = recordEditor.addField("break_length");
        fieldEditor2.putIntegerValue(5);

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "sport", "monday");
    }

    @Test
    public void testUpdateRecordInsertFieldViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.editRecord("monday");
        final FieldEditor fieldEditor1 = recordEditor.addField("break_start");
        fieldEditor1.putStringValue("3:30 p.m.");
        final FieldEditor fieldEditor2 = recordEditor.addField("break_length");
        fieldEditor2.putIntegerValue(5);

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_insert_field.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(expectedChangesList, actualChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testSetRecordSetField() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.setRecord("monday");
        final FieldEditor fieldEditor1 = recordEditor.addField("break_start");
        fieldEditor1.putStringValue("3:30 p.m.");
        final FieldEditor fieldEditor2 = recordEditor.addField("break_length");
        fieldEditor2.putIntegerValue(5);

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.SET, "sport", "monday");
    }

    @Test
    public void testSetRecordSetFieldViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        final RecordEditor recordEditor = collectionEditor.setRecord("monday");
        final FieldEditor fieldEditor1 = recordEditor.addField("break_start");
        fieldEditor1.putStringValue("3:30 p.m.");
        final FieldEditor fieldEditor2 = recordEditor.addField("break_length");
        fieldEditor2.putIntegerValue(5);

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/set_record_set_field.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(expectedChangesList, actualChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testDeleteRecord() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        collectionEditor.removeRecord("monday");

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.DELETE, "sport", "monday");
    }

    @Test
    public void testDeleteRecordViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("sport");
        collectionEditor.removeRecord("monday");

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList = getChangesList("changes/delete_record.json");
        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(expectedChangesList, actualChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordInsertListItem() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        final FieldEditor listItemEditor = fieldListEditor.insert(1);
        listItemEditor.putStringValue("mock_day_of_week");

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "calendar", "week");
    }

    @Test
    public void testUpdateRecordInsertListItemViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        final FieldEditor listItemEditor = fieldListEditor.insert(1);
        listItemEditor.putStringValue("mock_day_of_week");

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_insert_list_item.json");
        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(actualChangesList, expectedChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordSetListItem() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        final FieldEditor listItemEditor = fieldListEditor.set(1);
        listItemEditor.putStringValue("mock_day_of_week");

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "calendar", "week");
    }

    @Test
    public void testUpdateRecordSetListItemViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        final FieldEditor listItemEditor = fieldListEditor.set(1);
        listItemEditor.putStringValue("mock_day_of_week");

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_set_list_item.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(actualChangesList, expectedChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordDeleteListItem() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.delete(1);

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "calendar", "week");
        assertFieldListChanges(databaseWrapper,
                               FieldChangeType.LIST_ITEM_DELETE,
                               "calendar",
                               "week",
                               "days_of_weeks",
                               1);
    }

    @Test
    public void testUpdateRecordDeleteListItemViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.delete(1);

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_delete_list_item.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(actualChangesList, expectedChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordMoveUpListItem() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.moveTo(1, 5);

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "calendar", "week");
        assertFieldChanges(databaseWrapper,
                           FieldChangeType.LIST_ITEM_MOVE,
                           "calendar",
                           "week",
                           "days_of_weeks");
    }

    @Test
    public void testUpdateRecordMoveUpListItemViaRepository() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.moveTo(1, 5);

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_move_up_list_item.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(expectedChangesList, actualChangesList);

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordMoveDownListItem() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.moveTo(5, 1);

        snapshotEditor.commit();

        assertRecordChanges(databaseWrapper, RecordChangeType.UPDATE, "calendar", "week");
        assertFieldChanges(databaseWrapper,
                           FieldChangeType.LIST_ITEM_MOVE,
                           "calendar",
                           "week",
                           "days_of_weeks");
    }

    @Test
    public void testUpdateRecordMoveDownListItemViaRepository() throws Exception {

        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.editField("days_of_weeks");
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.moveTo(5, 1);

        snapshotEditor.commit();

        //TODO uncomment block after optimization of LocalChangesRepository
        /* final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_move_down_list_item.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(actualChangesList, expectedChangesList);*/

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "database_snapshot.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testUpdateRecordInsertList() throws Exception {
        fillDatabaseInfo("get_database_info.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final Snapshot snapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);
        final SnapshotEditor snapshotEditor = snapshot.edit();
        final CollectionEditor collectionEditor = snapshotEditor.editCollection("calendar");
        final RecordEditor recordEditor = collectionEditor.editRecord("week");
        final FieldEditor fieldEditor = recordEditor.addField("days_of_weeks_order");
        final FieldListEditor fieldListEditor = fieldEditor.putListValue();

        fieldListEditor.insert(0).putIntegerValue(0);
        fieldListEditor.insert(1).putIntegerValue(1);
        fieldListEditor.insert(2).putIntegerValue(2);
        fieldListEditor.insert(3).putIntegerValue(3);
        fieldListEditor.insert(4).putIntegerValue(4);
        fieldListEditor.insert(5).putIntegerValue(5);
        fieldListEditor.insert(6).putIntegerValue(6);

        snapshotEditor.commit();

        final List<ChangesDto> expectedChangesList =
                getChangesList("changes/update_record_insert_list.json");

        final List<ChangesDto> actualChangesList = localChangesRepository.get();

        assertChangesList(actualChangesList, expectedChangesList);
    }

    @Test
    public void testRevertArray() throws Exception {
        fillDatabaseInfo("apply_changes/original_database_info.json");

        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "apply_changes/snapshot_step_1.json");

        final FieldListEditor fieldListEditor2 = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID).edit()
                .editCollection("calendar")
                .editRecord("week")
                .editField("days_of_weeks")
                .editListValue();

        fieldListEditor2.moveTo(6, 0);
        fieldListEditor2.moveTo(6, 1);
        fieldListEditor2.moveTo(6, 2);
        fieldListEditor2.moveTo(6, 3);
        fieldListEditor2.moveTo(6, 4);
        fieldListEditor2.moveTo(6, 5);

        fieldListEditor2.commit();

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "apply_changes/snapshot_step_1.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testSortList() throws Exception {
        fillDatabaseInfo("apply_changes/original_database_info.json");

        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "apply_changes/snapshot_step_1.json");

        final FieldListEditor fieldListEditor2 = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID).edit()
                .editCollection("calendar")
                .editRecord("week")
                .editField("days_of_weeks")
                .editListValue();

        fieldListEditor2.moveTo(3, 6);
        fieldListEditor2.moveTo(2, 5);
        fieldListEditor2.moveTo(2, 4);
        fieldListEditor2.moveTo(0, 3);
        fieldListEditor2.moveTo(0, 1);

        fieldListEditor2.commit();

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "apply_changes/snapshot_step_1.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test
    public void testRemoveFirstAndAddToLast() throws Exception {
        fillDatabaseInfo("apply_changes/original_database_info.json");

        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "apply_changes/snapshot_step_1.json");

        final FieldListEditor fieldListEditor2 = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID).edit()
                .editCollection("calendar")
                .editRecord("week")
                .editField("days_of_weeks")
                .editListValue();

        fieldListEditor2.delete(0);
        fieldListEditor2.insert(7).putStringValue("last");

        fieldListEditor2.commit();

        fillDatabaseInfo("get_database_info_2.json");
        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID_2, "apply_changes/snapshot_step_1.json");

        applyChanges(MOCK_CONTEXT, MOCK_DATABASE_ID_2, localChangesRepository.get());

        final Snapshot actualSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID_2);
        final Snapshot expectedSnapshot = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID);

        assertSnapshotIgnoreRevision(actualSnapshot, expectedSnapshot);
    }

    @Test(expected = RecordAlreadyExistsException.class)
    public void testAlreadyExistsRecord() throws Exception {
        fillDatabaseInfo("apply_changes/original_database_info.json");

        fillDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID, "apply_changes/snapshot_step_1.json");

        final FieldEditor fieldEditor = getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID).edit()
                .addCollection("calendar")
                .addRecord("week")
                .addField("days_of_weeks");
        fieldEditor.putIntegerValue(100);

        fieldEditor.commit();
    }

    @Test(expected = RecordNotExistsException.class)
    public void testDeleteNotExitstRecord() throws Exception {
        fillDatabaseInfo("apply_changes/original_database_info.json");

        final CollectionEditor collectionEditor =
                getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID).edit().addCollection("calendar");
        collectionEditor.removeRecord("week");

        collectionEditor.commit();
    }

    @Test(expected = RecordNotExistsException.class)
    public void testUpdateNotExitstRecord() throws Exception {
        fillDatabaseInfo("apply_changes/original_database_info.json");

        final CollectionEditor collectionEditor =
                getSnapshot(MOCK_CONTEXT, MOCK_DATABASE_ID).edit().addCollection("calendar");
        collectionEditor.editRecord("week").addField("mock_field_id").putIntegerValue(100);

        collectionEditor.commit();
    }

    private void applyChanges(@NonNull final YDSContext databaseContext,
                              @NonNull final String databaseId,
                              @NonNull final List<ChangesDto> changesDtos) {
        final ChangesRepository changesRepository = new ChangesRepository(databaseManager,
                                                                          databaseContext,
                                                                          databaseId,
                                                                          true);

        final DeltaItemDto deltaItemDto = new DeltaItemDto();
        deltaItemDto.setChanges(changesDtos);
        deltaItemDto.setDeltaId(MOCK_DATABASE_ID);
        deltaItemDto.setRevision(1000);

        final List<DeltaItemDto> deltaItemDtoList = new ArrayList<>();
        deltaItemDtoList.add(deltaItemDto);

        final DeltasResponse deltasResponse = new DeltasResponse();
        deltasResponse.setItems(deltaItemDtoList);

        changesRepository.save(deltasResponse);
    }

    private Snapshot getSnapshot(@NonNull final YDSContext databaseContext,
                                 @NonNull final String databaseId) throws BaseException {
        final SnapshotRepository snapshotRepository =
                new SnapshotRepository(databaseManager, databaseContext, databaseId);
        return getSnapshot(snapshotRepository.get());
    }

    private Snapshot getSnapshot(@NonNull final SnapshotResponse snapshotResponse)
            throws BaseException {
        return new Snapshot(databaseManager,
                            MOCK_CONTEXT,
                            MOCK_DATABASE_ID,
                            processor,
                            snapshotResponse);
    }

    private void fillDatabase(@NonNull final YDSContext databaseContext,
                              @NonNull final String databaseId,
                              @NonNull final String fileName) throws IOException {
        final SnapshotRepository snapshotRepository =
                new SnapshotRepository(databaseManager, databaseContext, databaseId);

        final String jsonString = getTextFromFile(fileName);
        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class).fromJson(jsonString);
        snapshotRepository.save(snapshot);
    }

    private void fillDatabaseInfo(@NonNull final String fileName) throws Exception {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository databasesRepository = new DatabasesRepository(databaseWrapper);

        final String databaseInfoString = getTextFromFile(fileName);
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(databaseInfoString);
        databasesRepository.save(databaseDto);
    }

    private List<ChangesDto> getChangesList(@NonNull final String fileName) throws IOException {
        final String changesString = getTextFromFile(fileName);
        final DeltasResponse deltasResponse =
                new Moshi.Builder().build().adapter(DeltasResponse.class).fromJson(changesString);
        return deltasResponse.getItems().get(0).getChanges();
    }
}