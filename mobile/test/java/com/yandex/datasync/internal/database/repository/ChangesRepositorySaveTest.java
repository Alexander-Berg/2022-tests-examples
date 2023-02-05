/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.repository;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.Datatype;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.DeltaItemDto;
import com.yandex.datasync.internal.model.response.DeltasResponse;
import com.yandex.datasync.internal.model.response.FieldDto;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.RecordsDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.ImmediatelyOperationProcessor;
import com.yandex.datasync.util.ResourcesUtil;
import com.yandex.datasync.wrappedModels.Collection;
import com.yandex.datasync.wrappedModels.Record;
import com.yandex.datasync.wrappedModels.Snapshot;
import com.yandex.datasync.wrappedModels.Value;
import com.yandex.datasync.wrappedModels.ValuesList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.yandex.datasync.asserters.RecordDtoAsserter.assertRecordRevision;
import static com.yandex.datasync.asserters.RecordsDtoAsserter.assertRecords;
import static com.yandex.datasync.internal.database.sql.triggers.TriggersTestObjectFactory.fillDatabase;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("ConstantConditions")
public class ChangesRepositorySaveTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_USER_ID = "mock_user_id";

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final int LIST_POSITION_LOW = 1;

    private static final int LIST_POSITION_HIGH = 5;

    private SnapshotRepository snapshotRepository;

    private DatabaseManager databaseManager;

    private SQLiteDatabaseWrapper databaseWrapper;

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        fillDatabaseInfo();

        databaseWrapper = databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);

        snapshotRepository = new SnapshotRepository(databaseManager,
                                                    MOCK_CONTEXT,
                                                    MOCK_DATABASE_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testSaveChanges() throws IOException {
        final ChangesRepository changesRepository = new ChangesRepository(databaseManager,
                                                                          MOCK_CONTEXT,
                                                                          MOCK_DATABASE_ID,
                                                                          true);
        changesRepository.save(Collections.emptyList());
    }

    @Test
    public void testInsertRecordSetField() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        final DeltasResponse deltasResponse = applyChanges("changes/insert_record_set_field.json");

        final Snapshot snapshot = getSnapshotWrapper();
        assertTrue(snapshot.hasCollection("sport"));

        final Collection collection = snapshot.getCollection("sport");
        assertNotNull(collection);

        assertTrue(collection.hasRecord("meeting"));

        final Record record = collection.getRecord("meeting");
        assertNotNull(record);

        assertTrue(record.hasField("starts"));

        final Value startsValue = record.getValue("starts");
        assertNotNull(startsValue);
        assertThat(startsValue.getDatatype(), is(Datatype.STRING));
        assertThat(startsValue.getStringValue(), is("3 p.m."));

        assertTrue(record.hasField("finishes"));

        final Value finishesValue = record.getValue("finishes");
        assertNotNull(finishesValue);
        assertThat(finishesValue.getDatatype(), is(Datatype.INTEGER));
        assertThat(finishesValue.getIntegerValue(), is(4));

        assertRecordRevision(databaseWrapper, deltasResponse);
    }

    @Test
    public void testInsertRecordSetFieldViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();

        final DeltasResponse deltasResponse = applyChanges("changes/insert_record_set_field.json");

        final ValueDto value0 = new ValueDto();
        value0.setType(Datatype.STRING);
        value0.setStringValue("3 p.m.");

        final ValueDto value1 = new ValueDto();
        value1.setType(Datatype.INTEGER);
        value1.setIntegerValue(4);

        final FieldDto field0 = new FieldDto();
        field0.setFieldId("starts");
        field0.setValue(value0);

        final FieldDto field1 = new FieldDto();
        field1.setFieldId("finishes");
        field1.setValue(value1);

        final List<FieldDto> fieldList = new ArrayList<>();
        fieldList.add(field1);
        fieldList.add(field0);

        final RecordDto record = new RecordDto();
        record.setFields(fieldList);

        record.setCollectionId("sport");
        record.setRecordId("meeting");

        final List<RecordDto> recordList = expectedSnapshot.getRecords().getItems();
        recordList.add(record);

        updateRevisions(expectedSnapshot, deltasResponse);
        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testInsertRecordSetFieldNotExistingCollection() throws IOException, BaseException {

        applyChanges("changes/insert_record_set_field.json");

        final Snapshot snapshot = getSnapshotWrapper();
        assertFalse(snapshot.hasCollection("monday"));
    }

    @Test
    public void testUpdateRecordDeleteField() throws IOException, BaseException {

        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_delete_field.json");

        final Snapshot snapshot = getSnapshotWrapper();

        assertTrue(snapshot.hasCollection("sport"));

        final Collection collection = snapshot.getCollection("sport");
        assertNotNull(collection);

        assertTrue(collection.hasRecord("monday"));

        final Record record = collection.getRecord("monday");
        assertNotNull(record);

        assertFalse(record.hasField("starts"));
        assertTrue(record.hasField("ends"));

        assertRecordRevision(databaseWrapper, deltasResponse);
    }

    @Test
    public void testUpdateRecordDeleteFieldViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_delete_field.json");

        final RecordDto record = getRecordFromSnapshot(expectedSnapshot, "sport", "monday");
        final Iterator<FieldDto> fieldIterator = record.getFields().iterator();

        while (fieldIterator.hasNext()) {
            final FieldDto field = fieldIterator.next();
            if ("starts".equals(field.getFieldId())) {
                fieldIterator.remove();
            }
        }

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testUpdateRecordInsertField() throws IOException, BaseException {

        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_insert_field.json");

        final Snapshot snapshot = getSnapshotWrapper();

        assertTrue(snapshot.hasCollection("sport"));

        final Collection collection = snapshot.getCollection("sport");
        assertNotNull(collection);

        assertTrue(collection.hasRecord("monday"));
        final Record record = collection.getRecord("monday");
        assertNotNull(record);

        assertTrue(record.hasField("break_start"));

        final Value breakStartValue = record.getValue("break_start");
        assertNotNull(breakStartValue);
        assertThat(breakStartValue.getDatatype(), is(Datatype.STRING));
        assertThat(breakStartValue.getStringValue(), is("3:30 p.m."));

        assertTrue(record.hasField("break_length"));

        final Value breakLengthValue = record.getValue("break_length");
        assertNotNull(breakLengthValue);
        assertThat(breakLengthValue.getDatatype(), is(Datatype.INTEGER));
        assertThat(breakLengthValue.getIntegerValue(), is(5));

        assertTrue(record.hasField("starts"));
        assertTrue(record.hasField("ends"));

        assertRecordRevision(databaseWrapper, deltasResponse);
    }

    @Test
    public void testUpdateRecordInsertFieldViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_insert_field.json");

        final ValueDto value0 = new ValueDto();
        value0.setType(Datatype.STRING);
        value0.setStringValue("3:30 p.m.");

        final ValueDto value1 = new ValueDto();
        value1.setType(Datatype.INTEGER);
        value1.setIntegerValue(5);

        final FieldDto field0 = new FieldDto();
        field0.setFieldId("break_start");
        field0.setValue(value0);

        final FieldDto field1 = new FieldDto();
        field1.setFieldId("break_length");
        field1.setValue(value1);

        final RecordDto record = getRecordFromSnapshot(expectedSnapshot, "sport", "monday");
        final List<FieldDto> fieldList = record.getFields();
        fieldList.add(field1);
        fieldList.add(field0);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testSetRecordSetField() throws IOException, BaseException {

        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        final DeltasResponse deltasResponse = applyChanges("changes/set_record_set_field.json");

        final Snapshot snapshot = getSnapshotWrapper();

        assertTrue(snapshot.hasCollection("sport"));

        final Collection collection = snapshot.getCollection("sport");
        assertNotNull(collection);

        assertTrue(collection.hasRecord("monday"));
        final Record record = collection.getRecord("monday");
        assertNotNull(record);

        assertTrue(record.hasField("break_start"));

        final Value breakStartValue = record.getValue("break_start");
        assertNotNull(breakStartValue);
        assertThat(breakStartValue.getDatatype(), is(Datatype.STRING));
        assertThat(breakStartValue.getStringValue(), is("3:30 p.m."));

        assertTrue(record.hasField("break_length"));

        final Value breakLengthValue = record.getValue("break_length");
        assertNotNull(breakLengthValue);
        assertThat(breakLengthValue.getDatatype(), is(Datatype.INTEGER));
        assertThat(breakLengthValue.getIntegerValue(), is(5));

        assertFalse(record.hasField("starts"));
        assertFalse(record.hasField("ends"));

        assertRecordRevision(databaseWrapper, deltasResponse);
    }

    @Test
    public void testSetRecordSetFieldViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();

        final DeltasResponse deltasResponse = applyChanges("changes/set_record_set_field.json");

        final ValueDto value0 = new ValueDto();
        value0.setType(Datatype.STRING);
        value0.setStringValue("3:30 p.m.");

        final ValueDto value1 = new ValueDto();
        value1.setType(Datatype.INTEGER);
        value1.setIntegerValue(5);

        final FieldDto field0 = new FieldDto();
        field0.setFieldId("break_start");
        field0.setValue(value0);

        final FieldDto field1 = new FieldDto();
        field1.setFieldId("break_length");
        field1.setValue(value1);

        final RecordDto record = getRecordFromSnapshot(expectedSnapshot, "sport", "monday");
        final List<FieldDto> fieldList = record.getFields();
        fieldList.clear();
        fieldList.add(field1);
        fieldList.add(field0);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testDeleteRecord() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        final DeltasResponse deltasResponse = applyChanges("changes/delete_record.json");

        final Snapshot snapshot = getSnapshotWrapper();

        assertTrue(snapshot.hasCollection("sport"));

        final Collection collection = snapshot.getCollection("sport");
        assertNotNull(collection);

        assertFalse(collection.hasRecord("monday"));

        assertRecordRevision(databaseWrapper, deltasResponse);
    }

    @Test
    public void testDeleteRecordViaRepository() throws IOException, BaseException {
        final SnapshotResponse expectedSnapshot = snapshotRepository.get();

        applyChanges("changes/delete_record.json");

        final Iterator<RecordDto> recordIterator =
                expectedSnapshot.getRecords().getItems().iterator();
        while (recordIterator.hasNext()) {
            final RecordDto record = recordIterator.next();
            if ("monday".equals(record.getRecordId()) && "sport".equals(record.getCollectionId())) {
                recordIterator.remove();
            }
        }

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testUpdateRecordInsertListItem() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        applyChanges("changes/update_record_insert_list_item.json");

        final Snapshot snapshot = getSnapshotWrapper();
        final Collection collection = snapshot.getCollection("calendar");
        final Record record = collection.getRecord("week");
        final Value value = record.getValue("days_of_weeks");

        assertNotNull(value);
        assertThat(value.getDatatype(), is(Datatype.LIST));

        final ValuesList valueListWrapper = value.getListValue();
        assertNotNull(valueListWrapper);

        final Value dayOfWeek = valueListWrapper.getValue(LIST_POSITION_LOW);
        assertNotNull(dayOfWeek);
        assertThat(dayOfWeek.getDatatype(), is(Datatype.STRING));
        assertThat(dayOfWeek.getStringValue(), is("mock_day_of_week"));
    }

    @Test
    public void testUpdateRecordInsertListItemViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();
        final ValueDto value = getValueFromSnapshot(expectedSnapshot,
                                                    "calendar",
                                                    "week",
                                                    "days_of_weeks");

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_insert_list_item.json");

        final List<ValueDto> valueList = value.getListValues();

        final ValueDto newValue = new ValueDto();
        newValue.setType(Datatype.STRING);
        newValue.setStringValue("mock_day_of_week");
        valueList.add(LIST_POSITION_LOW, newValue);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testUpdateRecordSetListItem() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        applyChanges("changes/update_record_set_list_item.json");

        final Snapshot snapshot = getSnapshotWrapper();
        final Collection collection = snapshot.getCollection("calendar");
        final Record record = collection.getRecord("week");
        final Value value = record.getValue("days_of_weeks");

        assertNotNull(value);
        assertThat(value.getDatatype(), is(Datatype.LIST));

        final ValuesList valueListWrapper = value.getListValue();
        assertNotNull(valueListWrapper);

        final Value dayOfWeek = valueListWrapper.getValue(LIST_POSITION_LOW);
        assertNotNull(dayOfWeek);
        assertThat(dayOfWeek.getDatatype(), is(Datatype.STRING));
        assertThat(dayOfWeek.getStringValue(), is("mock_day_of_week"));
    }

    @Test
    public void testUpdateRecordSetListItemViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();
        final ValueDto value = getValueFromSnapshot(expectedSnapshot,
                                                    "calendar",
                                                    "week",
                                                    "days_of_weeks");

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_set_list_item.json");

        final List<ValueDto> valueList = value.getListValues();

        final ValueDto newValue = new ValueDto();
        newValue.setType(Datatype.STRING);
        newValue.setStringValue("mock_day_of_week");
        valueList.set(LIST_POSITION_LOW, newValue);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testUpdateRecordDeleteListItem() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        applyChanges("changes/update_record_delete_list_item.json");

        final Snapshot snapshot = getSnapshotWrapper();
        final Collection collection = snapshot.getCollection("calendar");
        final Record record = collection.getRecord("week");
        final Value value = record.getValue("days_of_weeks");

        assertNotNull(value);
        assertThat(value.getDatatype(), is(Datatype.LIST));

        final ValuesList valueListWrapper = value.getListValue();
        assertNotNull(valueListWrapper);

        final Value dayOfWeek0 = valueListWrapper.getValue(LIST_POSITION_LOW);
        assertNotNull(dayOfWeek0);
        assertThat(dayOfWeek0.getDatatype(), is(Datatype.STRING));
        assertThat(dayOfWeek0.getStringValue(), is(not("Monday")));
    }

    @Test
    public void testUpdateRecordDeleteListItemViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();
        final ValueDto value = getValueFromSnapshot(expectedSnapshot,
                                                    "calendar",
                                                    "week",
                                                    "days_of_weeks");

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_delete_list_item.json");

        final List<ValueDto> valueList = value.getListValues();
        valueList.remove(LIST_POSITION_LOW);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testUpdateRecordMoveUpListItem() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        applyChanges("changes/update_record_move_up_list_item.json");

        final Snapshot snapshot = getSnapshotWrapper();
        final Collection collection = snapshot.getCollection("calendar");
        final Record record = collection.getRecord("week");
        final Value value = record.getValue("days_of_weeks");

        assertNotNull(value);
        assertThat(value.getDatatype(), is(Datatype.LIST));

        final ValuesList valueListWrapper = value.getListValue();
        assertNotNull(valueListWrapper);

        final Value dayOfWeek = valueListWrapper.getValue(LIST_POSITION_HIGH);
        assertNotNull(dayOfWeek);
        assertThat(dayOfWeek.getDatatype(), is(Datatype.STRING));
        assertThat(dayOfWeek.getStringValue(), is("Monday"));
    }

    @Test
    public void testUpdateRecordMoveUpListItemViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();
        final ValueDto value = getValueFromSnapshot(expectedSnapshot,
                                                    "calendar",
                                                    "week",
                                                    "days_of_weeks");

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_move_up_list_item.json");

        final List<ValueDto> valueList = value.getListValues();
        final ValueDto valueForMove = valueList.remove(LIST_POSITION_LOW);
        valueList.add(LIST_POSITION_HIGH, valueForMove);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void testUpdateRecordMoveDownListItem() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");
        applyChanges("changes/update_record_move_down_list_item.json");

        final Snapshot snapshot = getSnapshotWrapper();
        final Collection collection = snapshot.getCollection("calendar");
        final Record record = collection.getRecord("week");
        final Value value = record.getValue("days_of_weeks");

        assertNotNull(value);
        assertThat(value.getDatatype(), is(Datatype.LIST));

        final ValuesList valueListWrapper = value.getListValue();
        assertNotNull(valueListWrapper);

        final Value dayOfWeek = valueListWrapper.getValue(LIST_POSITION_LOW);
        assertNotNull(dayOfWeek);
        assertThat(dayOfWeek.getDatatype(), is(Datatype.STRING));
        assertThat(dayOfWeek.getStringValue(), is("Friday"));
    }

    @Test
    public void testUpdateRecordMoveDownListItemViaRepository() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        final SnapshotResponse expectedSnapshot = snapshotRepository.get();
        final ValueDto value = getValueFromSnapshot(expectedSnapshot,
                                                    "calendar",
                                                    "week",
                                                    "days_of_weeks");

        final DeltasResponse deltasResponse =
                applyChanges("changes/update_record_move_down_list_item.json");

        final List<ValueDto> valueList = value.getListValues();
        final ValueDto valueForMove = valueList.remove(LIST_POSITION_HIGH);
        valueList.add(LIST_POSITION_LOW, valueForMove);

        updateRevisions(expectedSnapshot, deltasResponse);

        final SnapshotResponse actual = snapshotRepository.get();
        assertRecords(actual.getRecords(), expectedSnapshot.getRecords());
    }

    @Test
    public void shouldAddNewCollectionWhileApplyDeltas() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        applyChanges("changes/insert_new_collection_one_field.json");

        final SnapshotResponse actual = snapshotRepository.get();
        assertThat(actual.getRecordsCount(), equalTo(5L));
        final RecordDto recordFromDelta = actual.getRecords().getItems().get(4);
        assertThat(recordFromDelta.getCollectionId(), equalTo("new_collection"));
        assertThat(recordFromDelta.getRecordId(), equalTo("test_id"));
        assertThat(recordFromDelta.getRevision(), equalTo(2L));
    }

    @Test
    public void shouldNotAddNewCollectionWhileApplyDeltas() throws IOException, BaseException {
        fillDatabase(databaseManager, MOCK_CONTEXT, MOCK_DATABASE_ID, "database_snapshot.json");

        applyChanges("changes/insert_new_collection_one_field.json", true);

        final SnapshotResponse actual = snapshotRepository.get();
        assertThat(actual.getRecordsCount(), equalTo(4L));
        final Set<String> itemsCollections = new HashSet<>();
        for (final RecordDto item : actual.getRecords().getItems()) {
            itemsCollections.add(item.getCollectionId());
        }

        assertThat(itemsCollections, not(contains("new_collection")));
    }

    private void updateRevisions(@NonNull final SnapshotResponse snapshotResponse,
                                 @NonNull final DeltasResponse deltas) {
        snapshotResponse.setRevision(deltas.getRevision());

        for (final DeltaItemDto deltaItem : deltas.getItems()) {
            final long revision = deltaItem.getRevision();
            for (final ChangesDto changes : deltaItem.getChanges()) {
                final String collectionId = changes.getCollectionId();
                final String recordId = changes.getRecordId();
                updateRevision(snapshotResponse.getRecords(), collectionId, recordId, revision);
            }
        }
    }

    private void updateRevision(@NonNull final RecordsDto recordsDto,
                                @NonNull final String collectionId,
                                @NonNull final String recordId,
                                final long revision) {
        for (final RecordDto record : recordsDto.getItems()) {
            if (collectionId.equals(record.getCollectionId())
                && recordId.equals(record.getRecordId())) {
                record.setRevision(revision);
            }
        }
    }

    private Snapshot getSnapshotWrapper() throws BaseException {

        final SnapshotResponse snapshot = snapshotRepository.get();

        return new Snapshot(databaseManager,
                            MOCK_CONTEXT,
                            MOCK_DATABASE_ID,
                            new ImmediatelyOperationProcessor(),
                            snapshot);
    }

    private DeltasResponse applyChanges(@NonNull final String changesFilePath) throws IOException {
        return applyChanges(changesFilePath, false);
    }

    private DeltasResponse applyChanges(@NonNull final String changesFilePath,
                                        final boolean shouldSkipUnknownCollectionsDeltas) throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile(changesFilePath);
        final DeltasResponse deltas =
                new Moshi.Builder().build().adapter(DeltasResponse.class).fromJson(jsonString);

        final ChangesRepository changesRepository = new ChangesRepository(databaseManager,
                                                                          MOCK_CONTEXT,
                                                                          MOCK_DATABASE_ID,
                                                                          shouldSkipUnknownCollectionsDeltas);
        changesRepository.save(deltas);

        return deltas;
    }

    private ValueDto getValueFromSnapshot(final SnapshotResponse snapshot,
                                          final String collectionId,
                                          final String recordId,
                                          final String fieldId) {
        final RecordsDto records = snapshot.getRecords();
        ValueDto result = null;
        for (final RecordDto record : records.getItems()) {
            if (collectionId.equals(record.getCollectionId())) {
                if (recordId.equals(record.getRecordId())) {
                    for (final FieldDto field : record.getFields()) {
                        if (fieldId.equals(field.getFieldId())) {
                            result = field.getValue();
                        }
                    }
                }
            }
        }
        return result;
    }

    private RecordDto getRecordFromSnapshot(final SnapshotResponse snapshot,
                                            final String collectionId,
                                            final String recordId) {
        final RecordsDto records = snapshot.getRecords();
        RecordDto result = null;
        for (final RecordDto record : records.getItems()) {
            if (collectionId.equals(record.getCollectionId())) {
                if (recordId.equals(record.getRecordId())) {
                    result = record;
                }
            }
        }
        return result;
    }

    private void fillDatabaseInfo() throws IOException {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository changesRepository = new DatabasesRepository(databaseWrapper);
        final String databaseInfo = ResourcesUtil.getTextFromFile("get_database_info.json");
        final DatabaseDto databaseDto =
                new Moshi.Builder().build().adapter(DatabaseDto.class).fromJson(databaseInfo);
        changesRepository.save(databaseDto);
    }
}
