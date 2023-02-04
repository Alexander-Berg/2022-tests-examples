package ru.yandex.qe.dispenser.ws.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import ru.yandex.qe.dispenser.api.util.SerializationUtils;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

public abstract class ApiTestBase extends AcceptanceTestBase {
    protected void assertLastMethodEquals(@NotNull final String method) {
        Assertions.assertEquals(SpyWebClient.lastMethod(), method);
    }

    protected void assertLastPathEquals(@NotNull final String uri) {
        Assertions.assertEquals(SpyWebClient.lastPath(), uri, "Path's are different\n");
    }

    protected void assertLastPathQueryEquals(@NotNull final String pathQuery) {
        Assertions.assertEquals(SpyWebClient.lastPathQuery(), pathQuery, "Query's are different\n");
    }

    protected void assertLastRequestBodyEquals(@NotNull final String classpathToExpected) {
        assertJsonEquals(classpathToExpected, SpyWebClient.lastRequestBody());
    }

    protected void assertLastResponseStatusEquals(final int status) {
        Assertions.assertEquals(status, SpyWebClient.lastResponseStatus());
    }

    protected void assertLastResponseHeaderEquals(@NotNull final String name, @NotNull final String value) {
        Assertions.assertEquals(SpyWebClient.lastResponseHeader(name), value, "Headers are different\n");
    }

    protected void assertLastResponseEquals(@NotNull final String classpathToExpected) {
        assertJsonEquals(classpathToExpected, SpyWebClient.lastResponse());
    }

    protected void assertJsonEquals(@NotNull final String classpathToExpected, @NotNull final Object actual) {
        assertJsonEquals(classpathToExpected, actual, new TypeReference<JsonNode>() {
        });
    }

    protected <T> void assertJsonEquals(@NotNull final String classpathToExpected, @NotNull final Object actual, @NotNull final TypeReference<T> type) {
        final T expected = fromClasspath(classpathToExpected, type);
        if (Objects.equals(expected, actual)) {
            return;
        }
        final ObjectWriter writer = new ObjectMapper().registerModules(new KotlinModule.Builder().build(),
                new Jdk8Module(), new JavaTimeModule()).writerWithDefaultPrettyPrinter();
        final String message = "See in classpath: " + classpathToExpected + "\n";
        try {
            Assertions.assertEquals(writer.writeValueAsString(expected), writer.writeValueAsString(normalize(actual)), message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private Object normalize(@NotNull final Object actual) {
        try {
            return actual instanceof String ? SerializationUtils.readValue((String) actual, Object.class) : actual;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    protected JsonNode fromClasspath(@NotNull final String classpath) {
        return fromClasspath(classpath, new TypeReference<JsonNode>() {
        });
    }

    @NotNull
    protected <T> T fromClasspath(@NotNull final String classpath, @NotNull final TypeReference<T> type) {
        final InputStream in = getClass().getResourceAsStream(classpath);
        if (in == null) {
            throw new RuntimeException("No file in classpath: " + classpath);
        }
        try {
            return SerializationUtils.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
