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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RecordEditorTest {

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "MOCK_RECORD_ID";

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final String MOCK_FIELD_ID_2 = "mock_field_id_2";

    private static final String MOCK_STRING_VALUE = "mock_string_value";

    private static final int MOCK_INTEGER_VALUE = 100;

    @Mock
    private CollectionEditor parentEditor;

    @Before
    public void setUp() {
        initMocks(this);
        when(parentEditor.getCollectionId()).thenReturn(MOCK_COLLECTION_ID);
    }

    @Test
    public void testEditField() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);

        final FieldEditor fieldEditor = recordEditor.editField(MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final ChangesDto changes = recordEditor.build();

        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto.getValue());

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(expectedValue, changeDto.getValue());
    }

    @Test
    public void testEditSeveralFields() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);

        final FieldEditor fieldEditor1 = recordEditor.editField(MOCK_FIELD_ID);
        fieldEditor1.putStringValue(MOCK_STRING_VALUE);

        final FieldEditor fieldEditor2 = recordEditor.editField(MOCK_FIELD_ID_2);
        fieldEditor2.putIntegerValue(MOCK_INTEGER_VALUE);

        final ChangesDto changes = recordEditor.build();

        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto1.getValue());

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setStringValue(MOCK_STRING_VALUE);
        expectedValue1.setType(Datatype.STRING);

        assertValue(expectedValue1, changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID_2));
        assertThat(changeDto2.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto2.getValue());

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue2.setType(Datatype.INTEGER);

        assertValue(expectedValue2, changeDto2.getValue());
    }

    @Test
    public void testSetField() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);
        final FieldEditor fieldEditor = recordEditor.addField(MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final ChangesDto changes = recordEditor.build();
        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto.getValue());

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(expectedValue, changeDto.getValue());
    }

    @Test
    public void testSetSeveralFields() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);
        final FieldEditor fieldEditor1 = recordEditor.addField(MOCK_FIELD_ID);
        fieldEditor1.putStringValue(MOCK_STRING_VALUE);

        final FieldEditor fieldEditor2 = recordEditor.addField(MOCK_FIELD_ID_2);
        fieldEditor2.putIntegerValue(MOCK_INTEGER_VALUE);

        final ChangesDto changes = recordEditor.build();

        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto1.getValue());

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setStringValue(MOCK_STRING_VALUE);
        expectedValue1.setType(Datatype.STRING);

        assertValue(expectedValue1, changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID_2));
        assertThat(changeDto2.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto2.getValue());

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue2.setType(Datatype.INTEGER);

        assertValue(expectedValue2, changeDto2.getValue());
    }

    @Test
    public void testDeleteField() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);
        recordEditor.removeField(MOCK_FIELD_ID);

        final ChangesDto changes = recordEditor.build();
        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getChangeType(), is(FieldChangeType.DELETE));
        assertNull(changeDto.getValue());
    }

    @Test
    public void testDeleteSeveralFields() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);
        recordEditor.removeField(MOCK_FIELD_ID);
        recordEditor.removeField(MOCK_FIELD_ID_2);

        final ChangesDto changes = recordEditor.build();
        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.DELETE));
        assertNull(changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);
        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID_2));
        assertThat(changeDto2.getChangeType(), is(FieldChangeType.DELETE));
        assertNull(changeDto2.getValue());
    }

    @Test
    public void testEditOneFieldSeveralTimes1() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);

        final FieldEditor fieldEditor1 = recordEditor.editField(MOCK_FIELD_ID);
        fieldEditor1.putIntegerValue(MOCK_INTEGER_VALUE);

        final FieldEditor fieldEditor2 = recordEditor.editField(MOCK_FIELD_ID);
        fieldEditor2.putStringValue(MOCK_STRING_VALUE);

        final ChangesDto changes = recordEditor.build();

        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto.getValue());

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(expectedValue, changeDto.getValue());
    }

    @Test
    public void testEditOneFieldSeveralTimes2() {
        final RecordEditor recordEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);

        final FieldEditor fieldEditor1 = recordEditor.editField(MOCK_FIELD_ID);
        fieldEditor1.putStringValue(MOCK_STRING_VALUE);

        final FieldEditor fieldEditor2 = recordEditor.editField(MOCK_FIELD_ID);
        fieldEditor2.putIntegerValue(MOCK_INTEGER_VALUE);

        final ChangesDto changes = recordEditor.build();

        assertThat(changes.getChangeType(), is(RecordChangeType.UPDATE));
        assertThat(changes.getCollectionId(), is(MOCK_COLLECTION_ID));
        assertThat(changes.getRecordId(), is(MOCK_RECORD_ID));

        final List<ChangeDto> changesList = changes.getChangeList();
        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getChangeType(), is(FieldChangeType.SET));
        assertNotNull(changeDto.getValue());

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue.setType(Datatype.INTEGER);

        assertValue(expectedValue, changeDto.getValue());
    }

    @Test
    public void testCommit() {
        final RecordEditor fieldEditor = new RecordEditor(parentEditor, MOCK_RECORD_ID);
        fieldEditor.commit();

        verify(parentEditor).commit();
    }
}