package ru.yandex.infra.stage.util;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.function.Executable;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.yp.client.api.DataModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssertUtils {
    // TODO: this tests only for guarantee, because limit tests require more code (zero limit is not additive).
    public static void assertResourceRequestEquals(DataModel.TPodSpec.TResourceRequests result, AllComputeResources expect) {
        assertThatEquals(result.getMemoryGuarantee(), expect.getMemoryGuarantee());
        assertThatEquals(result.getVcpuGuarantee(), expect.getVcpuGuarantee());
    }

    public static void assertJsonStringEquals(String actual, String expected) {
        ObjectMapper mapper = JsonUtils.DEFAULT_MAPPER;
        try {
            JsonNode actualNode = mapper.readTree(actual);
            JsonNode expectedNode = mapper.readTree(expected);
            assertThatEquals(actualNode, expectedNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static <T> void assertThatSameInstance(T actual, T expected) {
        assertThat(actual, sameInstance(expected));
    }

    public static <T> void assertThatEquals(T actual, T expected) {
        assertThat(actual, equalTo(expected));
    }

    @SafeVarargs
    public static <T> void assertThatContains(Iterable<T> actualValues, T... expectedValues) {
        assertThat(actualValues, contains(expectedValues));
    }

    public static void assertThatThrowsWithMessage(Class<? extends Exception> exceptionClass, String expectedExceptionMessage, Executable executable) {
        var exception = assertThrows(exceptionClass, executable);
        assertThatEquals(exception.getMessage(), expectedExceptionMessage);
    }

    public static <T> void assertCollectionMatched(Collection<T> values, int expectedSize, Predicate<T> predicate) {
        assertThat(values.size(), equalTo(expectedSize));
        assertThat(values.stream().allMatch(predicate), equalTo(true));
    }
}
