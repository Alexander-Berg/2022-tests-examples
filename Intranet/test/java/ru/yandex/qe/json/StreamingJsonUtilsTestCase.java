package ru.yandex.qe.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexei Zakharov (ayza)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StreamingJsonUtilsTestCase {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(StreamingJsonUtilsTestCase.class);

    Path tempDir;
    final StreamingJsonUtils streamingJsonUtils = new StreamingJsonUtils(new DefaultJsonMapper());

    @BeforeAll
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @AfterAll
    public void tearDown() throws IOException {
        tempDir.toFile().deleteOnExit();
    }

    @Test
    public void testWriteStringArray() {
        List<String> test1 = Arrays.asList("1", "2", "3", "4", "5");
        File file1 = tempDir.resolve("testWrite1.json").toFile();
        streamingJsonUtils.writeArray(test1.iterator(), file1);
        List<String> result1 = JsonUtils.readList(file1, String.class);
        Assertions.assertEquals(result1, test1);
    }

    @Test
    public void testWriteEmptyArray() {
        File file2 = tempDir.resolve("testWrite2.json").toFile();
        streamingJsonUtils.writeArray(new ArrayList<String>().iterator(), file2);
        Assertions.assertEquals(JsonUtils.readList(file2, String.class).size(), 0);
    }

    @Test
    public void testWriteArrayOfObjects() {
        File file3 = tempDir.resolve("testWrite3.json").toFile();
        List<TestPojo> pojos = Arrays.asList(
                new TestPojo("3", 1, new String[]{"31", "31"}),
                new TestPojo("3", 2, new String[]{"32", "32"}),
                new TestPojo("3", 3, new String[]{"33", "33"}));
        streamingJsonUtils.writeArray(pojos.iterator(), file3);
        Assertions.assertEquals(JsonUtils.readList(file3, TestPojo.class), pojos);
    }

    @Test
    public void testWriteArrayOfArrays() {
        File file4 = tempDir.resolve("testWrite4.json").toFile();
        List<Strings> arrays = Arrays.asList(
                new Strings(Arrays.asList("31", "31")),
                new Strings(Arrays.asList("32", "32")),
                new Strings(Arrays.asList("33", "33"))
        );
        streamingJsonUtils.writeArray(arrays.iterator(), file4);
        Assertions.assertEquals(JsonUtils.readList(file4, Strings.class), arrays);
    }

    @Test
    public void testReadStringArray() {
        List<String> test1 = Arrays.asList("q", "w", "e", "r", "t", "y");
        File file1 = tempDir.resolve("testRead1.json").toFile();
        JsonUtils.write(test1, file1);
        List<String> result1 = streamingReadToList(file1, String.class);
        Assertions.assertEquals(result1, test1);
    }

    @Test
    public void testReadEmptyArray() {
        File file2 = tempDir.resolve("testRead2.json").toFile();
        JsonUtils.write(new ArrayList<String>(), file2);
        Assertions.assertEquals(streamingReadToList(file2, String.class).size(), 0);
    }

    @Test
    public void testReadArrayOfObjects() {
        File file3 = tempDir.resolve("testRead3.json").toFile();
        List<TestPojo> pojos = Arrays.asList(
                new TestPojo("a", 0, new String[]{"a", "aa"}),
                new TestPojo("b", 3, new String[]{"b", "bb"}),
                new TestPojo("c", 7, new String[]{"c", "cc"}));
        JsonUtils.write(pojos, file3);
        Assertions.assertEquals(streamingReadToList(file3, TestPojo.class), pojos);

    }

    @Test
    public void testReadNotArray() {
        File file4 = tempDir.resolve("testRead4.json").toFile();
        TestPojo pojo4 = new TestPojo("1", 2, new String[]{"3", "4"});
        JsonUtils.write(pojo4, file4);
        try {
            streamingJsonUtils.readArray(file4, TestPojo.class);
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testReadArrayOfArrays() {
        // arrays of arrays
        File file5 = tempDir.resolve("testRead5.json").toFile();
        List<Strings> arrays = Arrays.asList(
                new Strings(Arrays.asList("31", "31")),
                new Strings(Arrays.asList("32", "32")),
                new Strings(Arrays.asList("33", "33"))
        );
        JsonUtils.write(arrays, file5);
        List<Strings> result = streamingReadToList(file5, Strings.class);
        Assertions.assertEquals(result, arrays);
    }

    @Test
    public void testReadBigFile() {
        InputStream jsonInput = this.getClass().getClassLoader().getResourceAsStream("json/url_list.json");
        List<String> list = streamingReadToList(jsonInput, String.class);
        Assertions.assertEquals(list.size(), 1000);
    }

    private <T> List<T> streamingReadToList(InputStream jsonInput, Class<T> clazz) {
        return StreamingJsonUtils.toStream(streamingJsonUtils.readArray(jsonInput, clazz)).collect(Collectors.toList());
    }

    private <T> List<T> streamingReadToList(File f, Class<T> clazz) {
        return StreamingJsonUtils.toStream(streamingJsonUtils.readArray(f, clazz)).collect(Collectors.toList());
    }

    private static class TestPojo {
        public String value1;
        public int value2;
        public String[] value3;

        public TestPojo() {
        }

        public TestPojo(String value1, int value2, String[] value3) {
            this.value1 = value1;
            this.value2 = value2;
            this.value3 = value3;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

        public boolean equals(Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }

        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static class Strings {

        private final List<String> data;

        @JsonCreator
        public Strings(List<String> data) {
            this.data = Collections.unmodifiableList(data);
        }

        @JsonValue
        public List<String> getData() {
            return data;
        }

        @Override
        public String toString() {
            return data.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Strings strings = (Strings) o;
            return data != null ? data.equals(strings.data) : strings.data == null;

        }

        @Override
        public int hashCode() {
            return data != null ? data.hashCode() : 0;
        }
    }
}

