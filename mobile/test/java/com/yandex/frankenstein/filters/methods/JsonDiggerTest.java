package com.yandex.frankenstein.filters.methods;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonDiggerTest {

    @Test
    public void testGetDescription() {
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1", "key2", "key3"));
        assertThat(jsonDigger.getDescription()).isEqualTo("key1 : key2 : key3");
    }

    @Test
    public void testGetOneKeyWithList() {
        final String json = "{\"key1\":[\"value1\",\"value2\"]}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[\"value1\",\"value2\"]");
    }

    @Test
    public void testGetOneKeyWithInteger() {
        final String json = "{\"key1\":42}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[\"42\"]");
    }

    @Test
    public void testGetOneKeyWithString() {
        final String json = "{\"key1\":\"42\"}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[\"42\"]");
    }

    @Test
    public void testGetWithoutKey() {
        final String json = "{\"key1\":\"42\"}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("different_key"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[]");
    }

    @Test
    public void testGetTwoKeysWithList() {
        final String json = "{\"key1\":{\"key2\":[\"value1\",\"value2\"]}}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1", "key2"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[\"value1\",\"value2\"]");
    }

    @Test
    public void testGetTwoKeysWithInteger() {
        final String json = "{\"key1\":{\"key2\":42}}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1", "key2"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[\"42\"]");
    }

    @Test
    public void testGetTwoKeysWithString() {
        final String json = "{\"key1\":{\"key2\":\"42\"}}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1", "key2"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[\"42\"]");
    }

    @Test
    public void testGetTwoKeysWithoutSecondKey() {
        final String json = "{\"key1\":{\"key2\":\"42\"}}";
        final JsonDigger jsonDigger = new JsonDigger(Arrays.asList("key1", "different_key"));
        assertThat(jsonDigger.get(new JSONObject(json)).toString()).isEqualTo("[]");
    }
}
