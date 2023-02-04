package ru.yandex.qe.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JsonUtilsTestCase {

    Path tempDir;

    @BeforeAll
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @AfterAll
    public void tearDown() throws IOException {
        tempDir.toFile().deleteOnExit();
    }

    @Test
    public void validate_true() {
        boolean result = JsonUtils.validate("{\"a\":1}");
        assertThat(result, Matchers.is(true));
    }

    @Test
    public void validate_false() {
        boolean result = JsonUtils.validate("{a:1}");
        assertThat(result, Matchers.is(false));
    }

    @Test
    public void validate_null() {
        boolean result = JsonUtils.validate(null);
        //noinspection ConstantConditions
        assertThat(result, Matchers.is(false));
    }

    @Test
    public void write_null() {
        String result = JsonUtils.write(null);
        //noinspection ConstantConditions
        assertThat(result, Matchers.is("null"));
    }

    @Test
    public void write_empty() {
        String result = JsonUtils.write("");
        //noinspection ConstantConditions
        assertThat(result, Matchers.is("\"\""));
    }

    @Test
    public void write_simple() {
        String result = JsonUtils.write(new SimpleObject("simple"));
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.is("{\"value\":\"simple\"}"));
    }

    @Test
    public void write_list() {
        String result = JsonUtils.write(Collections.singletonList(new SimpleObject("simple")));
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.is("[{\"value\":\"simple\"}]"));
    }

    @Test
    public void write_map() {
        String result = JsonUtils.write(Collections.singletonMap("simple", new SimpleObject("simple")));
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.is("{\"simple\":{\"value\":\"simple\"}}"));
    }

    @Test
    public void write_array() {
        // string array
        List<String> test1 = Arrays.asList("7", "6", "5", "4", "3");
        File file1 = tempDir.resolve("write_array.json").toFile();
        JsonUtils.write(test1.iterator(), file1);
        List<String> result1 = JsonUtils.readList(file1, String.class);
        assertEquals(result1, test1);
    }

    @Test
    public void read_simple() {
        SimpleObject result = JsonUtils.read("{\"value\":\"simple\"}", SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result.getValue(), Matchers.is("simple"));
    }

    @Test
    public void read_type_reference() {
        String json =
            "["
            + "  ["
            + "    {\"value\": \"simple1\"},"
            + "    {\"value\": \"simple2\"}"
            + "  ],"
            + "  ["
            + "    {\"value\": \"simple3\"},"
            + "    {\"value\": \"simple4\"}\n"
            + "  ]"
            + "]";
        List<List<SimpleObject>> actual = JsonUtils.read(json, new TypeReference<List<List<SimpleObject>>>() {});

        List<List<SimpleObject>> expected = Arrays.asList(
            Arrays.asList(new SimpleObject("simple1"), new SimpleObject("simple2")),
            Arrays.asList(new SimpleObject("simple3"), new SimpleObject("simple4"))
        );
        assertEquals(expected, actual);
    }

    @Test
    public void read_null() {
        SimpleObject result = JsonUtils.read((String) null, SimpleObject.class);
        assertThat(result, Matchers.nullValue());
    }

    @Test
    public void read_null2() {
        SimpleObject result = JsonUtils.read("null", SimpleObject.class);
        assertThat(result, Matchers.nullValue());
    }

    @Test
    public void read_null_value_class() {
        //noinspection ConstantConditions
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtils.read((String) null, (Class) null);
        });
    }

    @Test
    public void test_null_type_reference() {
        List<List<SimpleObject>> result = JsonUtils.read((String) null, new TypeReference<List<List<SimpleObject>>>() {});
        assertNull(result);
    }

    @Test
    public void test_null_type_reference2() {
        List<List<SimpleObject>> result = JsonUtils.read("null", new TypeReference<List<List<SimpleObject>>>() {});
        assertNull(result);
    }

    @Test
    public void read_null_value_type_reference() {
        //noinspection ConstantConditions
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtils.read((String) null, (TypeReference) null);
        });
    }

    @Test
    public void readList_simple() {
        List<SimpleObject> result = JsonUtils.readList("[{\"value\":\"simple\"}]", SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.hasSize(1));
        assertThat(result.get(0).getValue(), Matchers.is("simple"));
    }

    @Test
    public void readList_simple_empty_list() {
        List<SimpleObject> result = JsonUtils.readList("[]", SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.hasSize(0));
    }

    @Test
    public void readList_null() {
        List<SimpleObject> result = JsonUtils.readList((String)null, SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.hasSize(0));
    }

    @Test
    public void readList_null2() {
        List<SimpleObject> result = JsonUtils.readList("null", SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result, Matchers.hasSize(0));
    }

    @Test
    public void readList_null_value_class() {
        //noinspection ConstantConditions
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtils.readList((String)null, null);
        });
    }

    @Test
    public void readMap_simple() {
        Map<String, SimpleObject> result = JsonUtils.readMap("{\"simple\":{\"value\":\"simple\"}}", String.class, SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result.size(), Matchers.is(1));
        assertThat(result.keySet().iterator().next(), Matchers.is("simple"));
        assertThat(result.get(result.keySet().iterator().next()), Matchers.notNullValue());
        assertThat(result.get(result.keySet().iterator().next()).getValue(), Matchers.is("simple"));
    }

    @Test
    public void readMap_simple_empty_map() {
        Map<String, SimpleObject> result = JsonUtils.readMap("{}", String.class, SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result.size(), Matchers.is(0));
    }

    @Test
    public void readMap_null() {
        Map<String, SimpleObject> result = JsonUtils.readMap((String)null, String.class, SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result.size(), Matchers.is(0));
    }

    @Test
    public void readMap_null2() {
        Map<String, SimpleObject> result = JsonUtils.readMap("null", String.class, SimpleObject.class);
        assertThat(result, Matchers.notNullValue());
        assertThat(result.size(), Matchers.is(0));
    }

    @Test
    public void readMap_null_key_class() {
        //noinspection ConstantConditions
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtils.readMap((String)null, null, SimpleObject.class);
        });
    }

    @Test
    public void readMap_null_value_class() {
        //noinspection ConstantConditions
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtils.readMap((String)null, String.class, null);
        });
    }

    @Test
    public void forEachArrayElement_basic() {
        // string array
        List<String> test = Arrays.asList("c", "a", "f", "e", "b", "a", "b", "e");
        File file = tempDir.resolve("forEachArrayElement_basic.json").toFile();
        JsonUtils.write(test, file);
        List<String> result =  new ArrayList<>();
        JsonUtils.forEachArrayElement(file, String.class, result::add);
        assertEquals(test, result);
    }

    private static class SimpleObject {
        private String value;

        protected SimpleObject() {
        }

        public SimpleObject(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimpleObject that = (SimpleObject) o;

            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
}
