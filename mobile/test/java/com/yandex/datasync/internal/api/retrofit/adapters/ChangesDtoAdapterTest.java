/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api.retrofit.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ChangeDto;
import com.yandex.datasync.internal.model.ChangesDto;
import com.yandex.datasync.internal.model.FieldChangeType;
import com.yandex.datasync.internal.model.RecordChangeType;
import com.yandex.datasync.internal.model.ValueDto;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangesDto.CHANGES;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangesDto.CHANGE_TYPE;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangesDto.COLLECTION_ID;
import static com.yandex.datasync.internal.api.retrofit.ModelDescriptor.ChangesDto.RECORD_ID;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ChangesDtoAdapterTest {

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final int MOCK_LIST_ITEM = 10;

    private static final int MOCK_LIST_ITEM_DEST = 100;

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "mock_record_id";

    private JsonAdapter<ChangesDto> adapter;

    @Before
    public void setUp() {
        adapter = new Moshi.Builder()
                .add(ChangesDto.class, new ChangesDtoAdapter()).build()
                .adapter(ChangesDto.class);
    }

    @Test
    public void testDeleteRecord() throws Exception {

        final ChangesDto changesDto = getChanges();
        changesDto.setChangeType(RecordChangeType.DELETE);

        final JSONObject jsonObject = new JSONObject(adapter.toJson(changesDto));

        assertTrue(jsonObject.has(COLLECTION_ID));
        assertThat(jsonObject.get(COLLECTION_ID), is(MOCK_COLLECTION_ID));

        assertTrue(jsonObject.has(RECORD_ID));
        assertThat(jsonObject.get(RECORD_ID), is(MOCK_RECORD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(RecordChangeType.DELETE.name()));

        assertFalse(jsonObject.has(CHANGES));
    }

    @Test
    public void testInsertRecord() throws Exception {

        final ChangesDto changesDto = getChanges();
        changesDto.setChangeType(RecordChangeType.INSERT);

        final JSONObject jsonObject = new JSONObject(adapter.toJson(changesDto));

        assertTrue(jsonObject.has(COLLECTION_ID));
        assertThat(jsonObject.get(COLLECTION_ID), is(MOCK_COLLECTION_ID));

        assertTrue(jsonObject.has(RECORD_ID));
        assertThat(jsonObject.get(RECORD_ID), is(MOCK_RECORD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(RecordChangeType.INSERT.name()));

        assertTrue(jsonObject.has(CHANGES));
        assertThat(jsonObject.getJSONArray(CHANGES).length(), is(1));
    }

    @Test
    public void testSetRecord() throws Exception {

        final ChangesDto changesDto = getChanges();
        changesDto.setChangeType(RecordChangeType.SET);

        final JSONObject jsonObject = new JSONObject(adapter.toJson(changesDto));

        assertTrue(jsonObject.has(COLLECTION_ID));
        assertThat(jsonObject.get(COLLECTION_ID), is(MOCK_COLLECTION_ID));

        assertTrue(jsonObject.has(RECORD_ID));
        assertThat(jsonObject.get(RECORD_ID), is(MOCK_RECORD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(RecordChangeType.SET.name()));

        assertTrue(jsonObject.has(CHANGES));
        assertThat(jsonObject.getJSONArray(CHANGES).length(), is(1));
    }

    @Test
    public void testUpdateRecord() throws Exception {

        final ChangesDto changesDto = getChanges();
        changesDto.setChangeType(RecordChangeType.UPDATE);

        final JSONObject jsonObject = new JSONObject(adapter.toJson(changesDto));

        assertTrue(jsonObject.has(COLLECTION_ID));
        assertThat(jsonObject.get(COLLECTION_ID), is(MOCK_COLLECTION_ID));

        assertTrue(jsonObject.has(RECORD_ID));
        assertThat(jsonObject.get(RECORD_ID), is(MOCK_RECORD_ID));

        assertTrue(jsonObject.has(CHANGE_TYPE));
        assertThat(jsonObject.getString(CHANGE_TYPE),
                   equalToIgnoringCase(RecordChangeType.UPDATE.name()));

        assertTrue(jsonObject.has(CHANGES));
        assertThat(jsonObject.getJSONArray(CHANGES).length(), is(1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalStateException.class)
    public void testNullCollectionId() {
        final ChangesDto changesDto = getChanges();
        changesDto.setCollectionId(null);
        changesDto.setChangeType(RecordChangeType.UPDATE);

        final String jsonString = adapter.toJson(changesDto);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalStateException.class)
    public void testNullRecordId() {
        final ChangesDto changesDto = getChanges();
        changesDto.setRecordId(null);
        changesDto.setChangeType(RecordChangeType.UPDATE);

        final String jsonString = adapter.toJson(changesDto);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalStateException.class)
    public void testNullChangeType() {
        final ChangesDto changesDto = getChanges();
        changesDto.setChangeType(null);

        adapter.toJson(changesDto);
    }

    @Test
    public void testNoEscapedQuotes() throws JSONException {
        final ChangesDto changesDto = getChanges();
        changesDto.setChangeType(RecordChangeType.SET);
        final String jsonString = adapter.toJson(changesDto);

        final String escapedQuoteRegex = "\\{\\\\\"";
        Matcher matcher = Pattern.compile(escapedQuoteRegex).matcher(jsonString);
        assertFalse(matcher.find());
    }

    private ChangesDto getChanges() {
        final ChangeDto changeDto = getChangeDto();
        final List<ChangeDto> changeDtoList = new ArrayList<>();
        changeDtoList.add(changeDto);

        final ChangesDto result = new ChangesDto();
        result.setCollectionId(MOCK_COLLECTION_ID);
        result.setRecordId(MOCK_RECORD_ID);
        result.setChangeList(changeDtoList);
        return result;
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
        changeDto.setChangeType(FieldChangeType.SET);
        return changeDto;
    }
}