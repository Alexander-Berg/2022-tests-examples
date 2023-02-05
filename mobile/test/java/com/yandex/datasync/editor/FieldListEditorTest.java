/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.editor;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.ValueDto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.yandex.datasync.asserters.ValueDtoAsserter.assertValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class FieldListEditorTest {

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final int MOCK_POSITION = 140;

    private static final int MOCK_POSITION_2 = 280;

    private static final int MOCK_INTEGER_VALUE = 100;

    private static final String MOCK_STRING_VALUE = "mock_string_value";

    @Mock
    private Editor parentEditor;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testInsertListItem() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        final FieldEditor fieldEditor = fieldListEditor.insert(MOCK_POSITION);
        fieldEditor.putIntegerValue(MOCK_INTEGER_VALUE);

        final List<ChangeDto> changesList = fieldListEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getChangeType(), is(FieldChangeType.LIST_ITEM_INSERT));

        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getListItem(), is(MOCK_POSITION));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue.setType(Datatype.INTEGER);

        assertValue(expectedValue, changeDto.getValue());
    }

    @Test
    public void testInsertSeveralListItems() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        final FieldEditor fieldEditor1 = fieldListEditor.insert(MOCK_POSITION);
        fieldEditor1.putIntegerValue(MOCK_INTEGER_VALUE);

        final FieldEditor fieldEditor2 = fieldListEditor.insert(MOCK_POSITION_2);
        fieldEditor2.putStringValue(MOCK_STRING_VALUE);

        final List<ChangeDto> changesList = fieldListEditor.build();

        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.LIST_ITEM_INSERT));

        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto1.getListItem(), is(MOCK_POSITION));

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue1.setType(Datatype.INTEGER);

        assertValue(expectedValue1, changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);

        assertThat(changeDto2.getChangeType(), is(FieldChangeType.LIST_ITEM_INSERT));

        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto2.getListItem(), is(MOCK_POSITION_2));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setStringValue(MOCK_STRING_VALUE);
        expectedValue2.setType(Datatype.STRING);

        assertValue(expectedValue2, changeDto2.getValue());
    }

    @Test
    public void testSetListItem() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        final FieldEditor fieldEditor = fieldListEditor.set(MOCK_POSITION);

        fieldEditor.putStringValue(MOCK_STRING_VALUE);

        final List<ChangeDto> changesList = fieldListEditor.build();

        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getChangeType(), is(FieldChangeType.LIST_ITEM_SET));

        assertThat(changeDto.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto.getListItem(), is(MOCK_POSITION));

        final ValueDto expectedValue = new ValueDto();
        expectedValue.setStringValue(MOCK_STRING_VALUE);
        expectedValue.setType(Datatype.STRING);

        assertValue(expectedValue, changeDto.getValue());
    }

    @Test
    public void testSetSeveralListItems() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        final FieldEditor fieldEditor1 = fieldListEditor.set(MOCK_POSITION);
        fieldEditor1.putIntegerValue(MOCK_INTEGER_VALUE);

        final FieldEditor fieldEditor2 = fieldListEditor.set(MOCK_POSITION_2);
        fieldEditor2.putStringValue(MOCK_STRING_VALUE);

        final List<ChangeDto> changesList = fieldListEditor.build();

        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.LIST_ITEM_SET));

        assertThat(changeDto1.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto1.getListItem(), is(MOCK_POSITION));

        final ValueDto expectedValue1 = new ValueDto();
        expectedValue1.setIntegerValue(MOCK_INTEGER_VALUE);
        expectedValue1.setType(Datatype.INTEGER);

        assertValue(expectedValue1, changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);

        assertThat(changeDto2.getChangeType(), is(FieldChangeType.LIST_ITEM_SET));

        assertThat(changeDto2.getFieldId(), is(MOCK_FIELD_ID));
        assertThat(changeDto2.getListItem(), is(MOCK_POSITION_2));

        final ValueDto expectedValue2 = new ValueDto();
        expectedValue2.setStringValue(MOCK_STRING_VALUE);
        expectedValue2.setType(Datatype.STRING);

        assertValue(expectedValue2, changeDto2.getValue());
    }

    @Test
    public void testMoveListItemTo() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        fieldListEditor.moveTo(MOCK_POSITION, MOCK_POSITION_2);

        final List<ChangeDto> changesList = fieldListEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getChangeType(), is(FieldChangeType.LIST_ITEM_MOVE));
        assertThat(changeDto.getListItem(), is(MOCK_POSITION));
        assertThat(changeDto.getListItemDest(), is(MOCK_POSITION_2));
        assertNull(changeDto.getValue());
    }

    @Test
    public void testMoveSeveralListItemsTo() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        fieldListEditor.moveTo(MOCK_POSITION, MOCK_POSITION_2);
        fieldListEditor.moveTo(MOCK_POSITION_2, MOCK_POSITION);

        final List<ChangeDto> changesList = fieldListEditor.build();
        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.LIST_ITEM_MOVE));
        assertThat(changeDto1.getListItem(), is(MOCK_POSITION));
        assertThat(changeDto1.getListItemDest(), is(MOCK_POSITION_2));
        assertNull(changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);
        assertThat(changeDto2.getChangeType(), is(FieldChangeType.LIST_ITEM_MOVE));
        assertThat(changeDto2.getListItem(), is(MOCK_POSITION_2));
        assertThat(changeDto2.getListItemDest(), is(MOCK_POSITION));
        assertNull(changeDto2.getValue());
    }

    @Test
    public void testDeleteListItem() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        fieldListEditor.delete(MOCK_POSITION);

        final List<ChangeDto> changesList = fieldListEditor.build();
        assertThat(changesList.size(), is(1));

        final ChangeDto changeDto = changesList.get(0);
        assertThat(changeDto.getChangeType(), is(FieldChangeType.LIST_ITEM_DELETE));
        assertThat(changeDto.getListItem(), is(MOCK_POSITION));
        assertNull(changeDto.getValue());
    }

    @Test
    public void testDeleteSeveralListItems() throws Exception {
        final FieldListEditor fieldListEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        fieldListEditor.delete(MOCK_POSITION);
        fieldListEditor.delete(MOCK_POSITION_2);

        final List<ChangeDto> changesList = fieldListEditor.build();
        assertThat(changesList.size(), is(2));

        final ChangeDto changeDto1 = changesList.get(0);
        assertThat(changeDto1.getChangeType(), is(FieldChangeType.LIST_ITEM_DELETE));
        assertThat(changeDto1.getListItem(), is(MOCK_POSITION));
        assertNull(changeDto1.getValue());

        final ChangeDto changeDto2 = changesList.get(1);
        assertThat(changeDto2.getChangeType(), is(FieldChangeType.LIST_ITEM_DELETE));
        assertThat(changeDto2.getListItem(), is(MOCK_POSITION_2));
        assertNull(changeDto2.getValue());
    }

    @Test
    public void testCommit() throws Exception {
        final FieldListEditor fieldEditor = new FieldListEditor(parentEditor, MOCK_FIELD_ID);
        fieldEditor.commit();

        verify(parentEditor).commit();
    }
}