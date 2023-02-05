package ru.yandex.market;

import android.net.Uri;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class UriTest {

    @Test
    public void testReturnsSchemeWithoutSchemeSeparator() {
        final Uri uri = Uri.parse("https://yandex.ru");
        assertEquals("https", uri.getScheme());
    }

    @Test
    public void testReturnsEmptySetWhenNoQueryParameters() {
        final Uri uri = Uri.parse("https://yandex.ru");
        final Set<String> queryParameterNames = uri.getQueryParameterNames();
        assertNotNull(queryParameterNames);
        assertThat(queryParameterNames, empty());
    }

    @Test
    public void testReturnEmptyPathSegmentsListWhenNoPath() {
        final Uri uri = Uri.parse("https://yandex.ru");
        final List<String> pathSegments = uri.getPathSegments();
        assertNotNull(pathSegments);
        assertThat(pathSegments, empty());
    }

    @Test
    public void testParseEncodedUrl() {
        final Uri uri = Uri.parse("https://yandex.ru/pa%20th?pa%20ram=va%20lue#frag%20ment");
        assertThat(uri.getPathSegments(), contains("pa th"));
        assertThat(uri.getQueryParameterNames(), contains("pa ram"));
        assertEquals("va lue", uri.getQueryParameter("pa ram"));
        assertEquals("frag ment", uri.getFragment());
    }
}
