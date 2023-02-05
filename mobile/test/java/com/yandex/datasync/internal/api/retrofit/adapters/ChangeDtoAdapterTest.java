/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api.retrofit.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.ValueDto;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangeDto.CHANGE_TYPE;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangeDto.FIELD_ID;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangeDto.LIST_ITEM;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangeDto.LIST_ITEM_DEST;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangeDto.VALUE;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ChangeDtoAdapterTest {

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final int MOCK_LIST_ITEM = 10;

    private static final int MOCK_LIST_ITEM_DEST = 100;

    private JsonAdapter<ChangeDto> adapter;

    @Before
    public void setUp() {
        adapter = new Moshi.Builder()
                .add(ChangeDto.class, new ChangeDtoAdapter())
                .build().adapter(ChangeDto.class);
    }

    @Test
    public void testSetListItem() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.LIST_ITEM_SET);

        final String jsonString = adapter.toJson(changeDto);
        final JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has(FIELD_ID));
        assertThat(jsonObject.getString(FIELD_ID), is(MOCK_FIELD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(FieldChangeType.LIST_ITEM_SET.name()));

        assertTrue(jsonObject.has(LIST_ITEM));
        assertThat(jsonObject.getInt(LIST_ITEM), is(MOCK_LIST_ITEM));

        assertFalse(jsonObject.has(LIST_ITEM_DEST));

        assertTrue(jsonObject.has(VALUE));
    }

    @Test
    public void testInsertListItem() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.LIST_ITEM_INSERT);

        final String jsonString = adapter.toJson(changeDto);
        final JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has(FIELD_ID));
        assertThat(jsonObject.getString(FIELD_ID), is(MOCK_FIELD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(FieldChangeType.LIST_ITEM_INSERT.name()));

        assertTrue(jsonObject.has(LIST_ITEM));
        assertThat(jsonObject.getInt(LIST_ITEM), is(MOCK_LIST_ITEM));

        assertFalse(jsonObject.has(LIST_ITEM_DEST));

        assertTrue(jsonObject.has(VALUE));
    }

    @Test
    public void testDeleteListItem() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.LIST_ITEM_DELETE);

        final String jsonString = adapter.toJson(changeDto);
        final JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has(FIELD_ID));
        assertThat(jsonObject.getString(FIELD_ID), is(MOCK_FIELD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(FieldChangeType.LIST_ITEM_DELETE.name()));

        assertTrue(jsonObject.has(LIST_ITEM));
        assertThat(jsonObject.getInt(LIST_ITEM), is(MOCK_LIST_ITEM));

        assertFalse(jsonObject.has(LIST_ITEM_DEST));

        assertFalse(jsonObject.has(VALUE));
    }

    @Test
    public void testMoveListItem() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.LIST_ITEM_MOVE);

        final String jsonString = adapter.toJson(changeDto);
        final JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has(FIELD_ID));
        assertThat(jsonObject.getString(FIELD_ID), is(MOCK_FIELD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(FieldChangeType.LIST_ITEM_MOVE.name()));

        assertTrue(jsonObject.has(LIST_ITEM));
        assertThat(jsonObject.getInt(LIST_ITEM), is(MOCK_LIST_ITEM));

        assertTrue(jsonObject.has(LIST_ITEM_DEST));
        assertThat(jsonObject.getInt(LIST_ITEM_DEST), is(MOCK_LIST_ITEM_DEST));

        assertFalse(jsonObject.has(VALUE));
    }

    @Test
    public void testDelete() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.DELETE);

        final String jsonString = adapter.toJson(changeDto);
        final JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has(FIELD_ID));
        assertThat(jsonObject.getString(FIELD_ID), is(MOCK_FIELD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(FieldChangeType.DELETE.name()));

        assertFalse(jsonObject.has(LIST_ITEM));

        assertFalse(jsonObject.has(LIST_ITEM_DEST));

        assertFalse(jsonObject.has(VALUE));
    }

    @Test
    public void testSet() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.SET);

        final String jsonString = adapter.toJson(changeDto);
        final JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has(FIELD_ID));
        assertThat(jsonObject.getString(FIELD_ID), is(MOCK_FIELD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(FieldChangeType.SET.name()));

        assertFalse(jsonObject.has(LIST_ITEM));

        assertFalse(jsonObject.has(LIST_ITEM_DEST));

        assertTrue(jsonObject.has(VALUE));
    }

    @Test(expected = IllegalStateException.class)
    public void testNullChangeType() {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(null);
        final String jsonString = adapter.toJson(changeDto);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalStateException.class)
    public void testNullFieldId() {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.LIST_ITEM_INSERT);
        changeDto.setFieldId(null);
        final String jsonString = adapter.toJson(changeDto);
    }

    @Test
    public void testNoEscapedQuotes() throws JSONException {
        final ChangeDto changeDto = getChangeDto();
        changeDto.setChangeType(FieldChangeType.SET);
        final String jsonString = adapter.toJson(changeDto);

        final String escapedQuoteRegex = "\\{\\\\\"";
        Matcher matcher = Pattern.compile(escapedQuoteRegex).matcher(jsonString);
        assertFalse(matcher.find());
    }

    private ChangeDto getChangeDto() {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.INTEGER);
        value.setIntegerValue(10);

        final ChangeDto changeDto = new ChangeDto();
        changeDto.setFieldId(MOCK_FIELD_ID);
        changeDto.setValue(value);
        changeDto.setListItem(MOCK_LIST_ITEM);
        changeDto.setListItemDest(MOCK_LIST_ITEM_DEST);
        return changeDto;
    }
}