/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api.retrofit.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.yandex.datasync.Datatype;
import com.yandex.datasync.internal.model.ValueDto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.yandex.datasync.internal.util.Arrays2.asStringArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
@RunWith(RobolectricTestRunner.class)
public class ValueDtoTypeAdapterTest {

    private static final String MOCK_STRING = "mock_string";

    private static final String MOCK_DATE = "2015-03-05T10:01:10.512000+00:00";

    private static final int MOCK_NUM = 10;

    private static final boolean MOCK_BOOLEAN = true;

    private List<String> keys;

    private JsonAdapter<ValueDto> valueDtoAdapter;

    @Before
    public void setUp() {
        valueDtoAdapter = new Moshi.Builder()
                .add(ValueDto.class, new ValueDtoTypeAdapter())
                .build().adapter(ValueDto.class);

        keys = new ArrayList<>(Arrays.asList(asStringArray(Datatype.values())));
    }

    @Test
    public void testStringValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.STRING);
        value.setStringValue(MOCK_STRING);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optString(value.getType().name().toLowerCase()), is(MOCK_STRING));
    }

    @Test
    public void testIntegerValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.INTEGER);
        value.setIntegerValue(MOCK_NUM);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optInt(value.getType().name().toLowerCase()), is(MOCK_NUM));
    }

    @Test
    public void testBinaryValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.BINARY);
        value.setBinaryValue(MOCK_STRING);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optString(value.getType().name().toLowerCase()), is(MOCK_STRING));
    }

    @Test
    public void testDoubleValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.DOUBLE);
        value.setDoubleValue(MOCK_NUM);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optDouble(value.getType().name().toLowerCase()),
                   is((double) MOCK_NUM));
    }

    @Test
    public void testBooleanValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.BOOLEAN);
        value.setBooleanValue(MOCK_BOOLEAN);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optBoolean(value.getType().name().toLowerCase()), is(MOCK_BOOLEAN));
    }

    @Test
    public void testInfValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.INF);
        value.setNinfValue(MOCK_BOOLEAN);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optBoolean(value.getType().name().toLowerCase()), is(MOCK_BOOLEAN));
    }

    @Test
    public void testNinfValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.NINF);
        value.setNinfValue(MOCK_BOOLEAN);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optBoolean(value.getType().name().toLowerCase()), is(MOCK_BOOLEAN));
    }

    @Test
    public void testNullValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.NULL);
        value.setNullValue(MOCK_BOOLEAN);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optBoolean(value.getType().name().toLowerCase()), is(MOCK_BOOLEAN));
    }

    @Test
    public void testDatetimeValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.DATETIME);
        value.setDatetimeValue(MOCK_DATE);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optString(value.getType().name().toLowerCase()), is((MOCK_DATE)));
    }

    @Test
    public void testNanValue() throws JSONException {
        final ValueDto value = new ValueDto();
        value.setType(Datatype.NAN);
        value.setNanValue(MOCK_BOOLEAN);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        assertThat(jsonObject.optBoolean(value.getType().name().toLowerCase()), is(MOCK_BOOLEAN));
    }

    @Test
    public void testListValue() throws JSONException {
        final ValueDto listItem = new ValueDto();
        listItem.setType(Datatype.STRING);
        listItem.setStringValue(MOCK_STRING);

        final List<ValueDto> values = new ArrayList<>();
        values.add(listItem);

        final ValueDto value = new ValueDto();
        value.setType(Datatype.LIST);
        value.setListValues(values);

        final JSONObject jsonObject = new JSONObject(valueDtoAdapter.toJson(value));
        final Iterator<String> keyIterator = jsonObject.keys();

        keys.remove(value.getType().name());

        while (keyIterator.hasNext()) {
            final String actualKey = keyIterator.next().toUpperCase();
            assertThat(keys, not(hasItem((actualKey))));
        }

        final JSONArray array = jsonObject.optJSONArray(Datatype.LIST.name().toLowerCase());
        assertThat(array.length(), is(values.size()));

        final JSONObject jsonListItem = array.optJSONObject(0);

        assertThat(jsonListItem.optString(Datatype.STRING.name().toLowerCase()), is(MOCK_STRING));

        System.out.print(valueDtoAdapter.toJson(value));
    }
}