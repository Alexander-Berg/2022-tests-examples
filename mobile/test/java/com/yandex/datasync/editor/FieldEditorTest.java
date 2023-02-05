/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.editor;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.util.DateFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;
import java.util.List;

import static com.yandex.datasync.asserters.ValueDtoAsserter.assertValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class FieldEditorTest {

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final String MOCK_STRING_VALUE = "MOCK_STRING_VALUE";

    private static final byte MOCK_BINARY_DATA[] = "MOCK_STRING_VALUE".getBytes();

    private static final int MOCK_INTEGER_VALUE = 100;

    private static final double MOCK_DOUBLE_VALUE = 100d;

    private static final Date MOCK_DATETIME_VALUE = DateFormat.parse("2016-08-02T09:02:05.893000+00:00");

    private static final boolean MOCK_BOOLEAN_VALUE = true;

    private static final boolean MOCK_NAN_VALUE = true;

    private static final boolean MOCK_NINF_VALUE = true;

    private static final boolean MOCK_INF_VALUE = true;

    private static final boolean MOCK_NULL_VALUE = true;

    @Mock
    private Editor parentEditor;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testEmptyChange() {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertTrue(changesList.isEmpty());
    }

    @Test
    public void testPutStringValue() throws Exception {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutIntegerValue() throws Exception {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putIntegerValue(MOCK_INTEGER_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue.setType(Datatype.INTEGER);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutDoubleValue() throws Exception {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putDoubleValue(MOCK_DOUBLE_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setDoubleValue(MOCK_DOUBLE_VALUE);
        expectedValue.setType(Datatype.BINARY);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutBinaryValue() throws Exception {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putBinaryValue(MOCK_BINARY_DATA);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setBinaryValue(new String(MOCK_BINARY_DATA));
        expectedValue.setType(Datatype.BINARY);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutBooleanValue() throws Exception {

        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putBooleanValue(MOCK_BOOLEAN_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setBooleanValue(MOCK_BOOLEAN_VALUE);
        expectedValue.setType(Datatype.BOOLEAN);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutDateValue() throws Exception {

        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putDatetimeValue(MOCK_DATETIME_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setDatetimeValue(DateFormat.format(MOCK_DATETIME_VALUE));
        expectedValue.setType(Datatype.DATETIME);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutNanValue() throws Exception {

        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putNanValue(MOCK_NAN_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setNanValue(MOCK_NAN_VALUE);
        expectedValue.setType(Datatype.NAN);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutNinfValue() throws Exception {

        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putNinfValue(MOCK_NINF_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setNinfValue(MOCK_NINF_VALUE);
        expectedValue.setType(Datatype.NINF);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutInfValue() throws Exception {

        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putInfValue(MOCK_INF_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setNinfValue(MOCK_NINF_VALUE);
        expectedValue.setType(Datatype.INF);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutNullValue() throws Exception {

        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putNullValue(MOCK_NULL_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();

        assertNotNull(changesList);
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setNinfValue(MOCK_NULL_VALUE);
        expectedValue.setType(Datatype.NULL);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testEditListValue() {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        assertNotNull(fieldListEditor);
    }

    @Test
    public void testPutListValue() {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        final FieldListEditor fieldListEditor = fieldEditor.putListValue();
        assertNotNull(fieldListEditor);
    }

    @Test
    public void testEditListPutString() {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        final FieldListEditor fieldListEditor = fieldEditor.putListValue();
        fieldListEditor.set(0).putStringValue(MOCK_STRING_VALUE);
        fieldListEditor.set(1).putDoubleValue(MOCK_DOUBLE_VALUE);
        fieldEditor.putIntegerValue(MOCK_INTEGER_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangeDto change = changesList.get(0);
        assertThat(change.getChangeType(), is(FieldChangeType.SET));
        assertThat(change.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change.getListItem(), is(0));
        assertThat(change.getListItemDest(), is(0));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue.setType(Datatype.INTEGER);

        assertValue(expectedValue, change.getValue());
    }

    @Test
    public void testPutStringEditList() {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.putIntegerValue(MOCK_INTEGER_VALUE);

        final FieldListEditor fieldListEditor = fieldEditor.editListValue();
        fieldListEditor.set(0).putStringValue(MOCK_STRING_VALUE);
        fieldListEditor.set(1).putDoubleValue(MOCK_DOUBLE_VALUE);

        final List<ChangeDto> changesList = fieldEditor.build();
        assertThat(changesList.size(), is(2));

        final ChangeDto change1 = changesList.get(0);
        assertThat(change1.getChangeType(), is(FieldChangeType.LIST_ITEM_SET));
        assertThat(change1.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change1.getListItem(), is(0));
        assertThat(change1.getListItemDest(), is(0));

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setStringValue(MOCK_STRING_VALUE);
        expectedValue1.setType(Datatype.INTEGER);

        assertValue(expectedValue1, change1.getValue());


        final ChangeDto change2 = changesList.get(1);
        assertThat(change2.getChangeType(), is(FieldChangeType.LIST_ITEM_SET));
        assertThat(change2.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(change2.getListItem(), is(1));
        assertThat(change2.getListItemDest(), is(0));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setDoubleValue(MOCK_DOUBLE_VALUE);
        expectedValue2.setType(Datatype.DOUBLE);

        assertValue(expectedValue2, change2.getValue());
    }

    @Test
    public void testCommit() throws Exception {
        final FieldEditor fieldEditor = new FieldEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.commit();

        verify(parentEditor).commit();
    }
}