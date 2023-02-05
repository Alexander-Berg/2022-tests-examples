package ru.yandex.market;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import okhttp3.HttpUrl;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class HttpUrlTest {

    @Test
    public void testReturnsSchemeWithoutSchemeSeparator() {
        final HttpUrl httpUrl = HttpUrl.parse("https://yandex.ru");
        assertEquals("https", httpUrl.scheme());
    }

    @Test
    public void testReturnsEmptySetWhenNoQueryParameters() {
        final HttpUrl httpUrl = HttpUrl.parse("https://yandex.ru");
        final Set<String> queryParameterNames = httpUrl.queryParameterNames();
        assertNotNull(queryParameterNames);
        assertThat(queryParameterNames, empty());
    }

    @Test
    public void testReturnEmptyStringPathSegmentsListWhenNoPath() {
        final HttpUrl httpUrl = HttpUrl.parse("https://yandex.ru");
        final List<String> pathSegments = httpUrl.encodedPathSegments();
        assertNotNull(pathSegments);
        assertThat(pathSegments, contains(""));
    }

    @Test
    public void testReturnEmptyStringWhenTrailingPathSeparatorExists() {
        final HttpUrl httpUrl = HttpUrl.parse("https://yandex.ru/path/");
        assertThat(httpUrl.encodedPathSegments(), contains("path", ""));
    }

    @Test
    public void testReturnPathSeparatorForEmptyPath() {
        HttpUrl httpUrl = HttpUrl.parse("https://yandex.ru");
        String encodedPath = httpUrl.encodedPath();
        assertEquals("/", encodedPath);

        httpUrl = HttpUrl.parse("https://yandex.ru/");
        encodedPath = httpUrl.encodedPath();
        assertEquals("/", encodedPath);
    }

    @Test
    public void testParseEncodedUrl() {
        final HttpUrl httpUrl =
                HttpUrl.parse("https://yandex.ru/pa%20th?pa%20ram=va%20lue#frag%20ment");
        assertThat(httpUrl.pathSegments(), contains("pa th"));
        assertThat(httpUrl.queryParameterNames(), contains("pa ram"));
        assertEquals("va lue", httpUrl.queryParameter("pa ram"));
        assertEquals("frag ment", httpUrl.fragment());
    }
}
