/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.editor;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.RecordChangeType;
import com.yandex.datasync.internal.model.ValueDto;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static com.yandex.datasync.asserters.ValueDtoAsserter.assertValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class CollectionEditorTest {

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "mock_record_id";

    private static final String MOCK_RECORD_ID_2 = "mock_record_2";

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final String MOCK_FIELD_ID_2 = "mock_field_id_2";

    private static final String MOCK_STRING_VALUE = "mock_string_value";

    private static final int MOCK_INTEGER_VALUE = 100;

    @Mock
    private SnapshotEditor parentEditor;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testEditRecord() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor = collectionEditor.editRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor = recordEditor.addField(MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList = changesDto.getChangeList();

        assertThat(changeList.size(), is(1));
        final ChangeDto changeDto = changeList.get(0);

        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(changeDto.getValue(), expectedValue);
    }

    @Test
    public void testEditSeveralRecords() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor1 = collectionEditor.editRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor1 = recordEditor1.addField(MOCK_FIELD_ID);
        fieldEditor1.putStringValue(MOCK_STRING_VALUE);

        final RecordEditor recordEditor2 = collectionEditor.editRecord(MOCK_RECORD_ID_2);

        final FieldEditor fieldEditor2 = recordEditor2.addField(MOCK_FIELD_ID_2);
        fieldEditor2.putIntegerValue(MOCK_INTEGER_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(2));

        final ChangesDto changesDto1 = changesList.get(0);
        assertThat(changesDto1.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changesDto1.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto1.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList1 = changesDto1.getChangeList();

        assertThat(changeList1.size(), is(1));
        final ChangeDto changeDto1 = changeList1.get(0);

        assertThat(changeDto1.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setStringValue(MOCK_STRING_VALUE);
        expectedValue1.setType(Datatype.STRING);

        assertValue(changeDto1.getValue(), expectedValue1);

        final ChangesDto changesDto2 = changesList.get(1);
        assertThat(changesDto2.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changesDto2.getRecordId(), is(MOCK_RECORD_ID_2));
        assertThat(changesDto2.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList2 = changesDto2.getChangeList();

        assertThat(changeList2.size(), is(1));
        final ChangeDto changeDto2 = changeList2.get(0);

        assertThat(changeDto2.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID_2));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue2.setType(Datatype.INTEGER);

        assertValue(changeDto2.getValue(), expectedValue2);
    }

    @Test
    public void testAddRecord() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor = collectionEditor.addRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor = recordEditor.addField(MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getChangeType(), is(RecordChangeType.INSERT));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList = changesDto.getChangeList();

        assertThat(changeList.size(), is(1));
        final ChangeDto changeDto = changeList.get(0);

        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(changeDto.getValue(), expectedValue);
    }

    @Test
    public void testAddSeveralRecords() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor1 = collectionEditor.addRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor1 = recordEditor1.addField(MOCK_FIELD_ID);
        fieldEditor1.putStringValue(MOCK_STRING_VALUE);

        final RecordEditor recordEditor2 = collectionEditor.addRecord(MOCK_RECORD_ID_2);

        final FieldEditor fieldEditor2 = recordEditor2.addField(MOCK_FIELD_ID_2);
        fieldEditor2.putIntegerValue(MOCK_INTEGER_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(2));

        final ChangesDto changesDto1 = changesList.get(0);
        assertThat(changesDto1.getChangeType(), is(RecordChangeType.INSERT));
        assertThat(changesDto1.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto1.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList1 = changesDto1.getChangeList();

        assertThat(changeList1.size(), is(1));
        final ChangeDto changeDto1 = changeList1.get(0);

        assertThat(changeDto1.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setStringValue(MOCK_STRING_VALUE);
        expectedValue1.setType(Datatype.STRING);

        assertValue(changeDto1.getValue(), expectedValue1);

        final ChangesDto changesDto2 = changesList.get(1);
        assertThat(changesDto2.getChangeType(), is(RecordChangeType.INSERT));
        assertThat(changesDto2.getRecordId(), is(MOCK_RECORD_ID_2));
        assertThat(changesDto2.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList2 = changesDto2.getChangeList();

        assertThat(changeList2.size(), is(1));
        final ChangeDto changeDto2 = changeList2.get(0);

        assertThat(changeDto2.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID_2));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue2.setType(Datatype.INTEGER);

        assertValue(changeDto2.getValue(), expectedValue2);
    }

    @Test
    public void testSetRecord() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor = collectionEditor.setRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor = recordEditor.addField(MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getChangeType(), is(RecordChangeType.SET));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList = changesDto.getChangeList();

        assertThat(changeList.size(), is(1));
        final ChangeDto changeDto = changeList.get(0);

        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(changeDto.getValue(), expectedValue);
    }

    @Test
    public void testSetSeveralRecords() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor1 = collectionEditor.setRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor1 = recordEditor1.addField(MOCK_FIELD_ID);
        fieldEditor1.putStringValue(MOCK_STRING_VALUE);

        final RecordEditor recordEditor2 = collectionEditor.setRecord(MOCK_RECORD_ID_2);

        final FieldEditor fieldEditor2 = recordEditor2.addField(MOCK_FIELD_ID_2);
        fieldEditor2.putStringValue(MOCK_STRING_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(2));

        final ChangesDto changesDto1 = changesList.get(0);
        assertThat(changesDto1.getChangeType(), is(RecordChangeType.SET));
        assertThat(changesDto1.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto1.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList1 = changesDto1.getChangeList();

        assertThat(changeList1.size(), is(1));
        final ChangeDto changeDto1 = changeList1.get(0);

        assertThat(changeDto1.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(changeDto1.getValue(), expectedValue);

        final ChangesDto changesDto2 = changesList.get(0);
        assertThat(changesDto2.getChangeType(), is(RecordChangeType.SET));
        assertThat(changesDto2.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto2.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList2 = changesDto2.getChangeList();

        assertThat(changeList2.size(), is(1));
        final ChangeDto changeDto2 = changeList2.get(0);

        assertThat(changeDto2.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue2.setType(Datatype.INTEGER);

        assertValue(changeDto2.getValue(), expectedValue);
    }

    @Test
    public void testDeleteRecord() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        collectionEditor.removeRecord(MOCK_RECORD_ID);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getChangeType(), is(RecordChangeType.DELETE));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList = changesDto.getChangeList();

        assertThat(changeList.size(), is(0));
    }

    @Test
    public void testDeleteSeveralRecords() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        collectionEditor.removeRecord(MOCK_RECORD_ID);
        collectionEditor.removeRecord(MOCK_RECORD_ID_2);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(2));

        final ChangesDto changesDto1 = changesList.get(0);
        assertThat(changesDto1.getChangeType(), is(RecordChangeType.DELETE));
        assertThat(changesDto1.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto1.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList1 = changesDto1.getChangeList();

        assertThat(changeList1.size(), is(0));

        final ChangesDto changesDto2 = changesList.get(0);
        assertThat(changesDto2.getChangeType(), is(RecordChangeType.DELETE));
        assertThat(changesDto2.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto2.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList2 = changesDto2.getChangeList();

        assertThat(changeList2.size(), is(0));
    }

    @Test
    public void testEditOneRecordSeveralTimes() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);

        final RecordEditor recordEditor = collectionEditor.editRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor = recordEditor.addField(MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final RecordEditor recordEditor2 = collectionEditor.editRecord(MOCK_RECORD_ID);

        final FieldEditor fieldEditor2 = recordEditor2.addField(MOCK_FIELD_ID_2);
        fieldEditor2.putIntegerValue(MOCK_INTEGER_VALUE);

        final List<ChangesDto> changesList = collectionEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangesDto changesDto = changesList.get(0);
        assertThat(changesDto.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changesDto.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto.getCollectionId(), is(MOCK_COLLECTION_ID));

        final List<ChangeDto> changeList = changesDto.getChangeList();

        assertThat(changeList.size(), is(2));

        final ChangesDto changesDto1 = changesList.get(0);
        assertThat(changesDto1.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changesDto1.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto1.getCollectionId(), is(MOCK_COLLECTION_ID));

        final ChangeDto changeDto1 = changeList.get(0);

        assertThat(changeDto1.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setStringValue(MOCK_STRING_VALUE);
        expectedValue1.setType(Datatype.STRING);

        assertValue(changeDto1.getValue(), expectedValue1);

        final ChangesDto changesDto2 = changesList.get(0);
        assertThat(changesDto2.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changesDto2.getRecordId(), is(MOCK_RECORD_ID));
        assertThat(changesDto2.getCollectionId(), is(MOCK_COLLECTION_ID));

        final ChangeDto changeDto2 = changeList.get(1);

        assertThat(changeDto2.getChangeType(), is(FieldChangeType.SET));
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID_2));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue2.setType(Datatype.INTEGER);

        assertValue(changeDto2.getValue(), expectedValue2);
    }

    @Test
    public void testCommit() {
        final CollectionEditor collectionEditor = new CollectionEditor(parentEditor,
                                                                       MOCK_COLLECTION_ID);
        collectionEditor.commit();

        verify(parentEditor).commit();
    }
}