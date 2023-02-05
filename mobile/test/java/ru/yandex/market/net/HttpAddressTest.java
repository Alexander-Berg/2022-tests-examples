package ru.yandex.market.net;

import android.os.Build;

import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import ru.yandex.market.CommonFailMessages;
import ru.yandex.market.base.network.common.address.HttpAddress;
import ru.yandex.market.base.network.common.address.QueryMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class HttpAddressTest {

    @Test
    public void testCreateDefaultInstanceWithoutException() {
        try {
            HttpAddress.builder().build();
        } catch (Throwable throwable) {
            fail(CommonFailMessages.defaultInstance(HttpAddress.class, throwable));
        }
    }

    @Test
    public void testEmptyInstanceIsIndeedEmpty() {
        assertTrue(HttpAddress.empty().isEmpty());
    }

    @Test
    public void testEmptyInstanceStringRepresentationIsAlsoEmpty() {
        assertThat(HttpAddress.empty().asEncodedString(), isEmptyString());
    }

    @Test
    public void testNoExceptionsWhenSerializingToJson() {
        try {
            final HttpAddress httpAddress = HttpAddress.builder()
                    .scheme("https")
                    .host("yandex.ru")
                    .addPathSegment("something")
                    .addPathSegment("special")
                    .addQueryParameter("param", "value")
                    .fragment("withFragment")
                    .build();
            new GsonBuilder().create().toJson(httpAddress);
        } catch (Throwable exception) {
            fail(CommonFailMessages.jsonSerializationFailed(HttpAddress.class, exception));
        }
    }

    @Test
    public void testIsEmptyOrNullWorksAsExpected() {
        assertTrue(HttpAddress.isEmptyOrNull(null));
        assertTrue(HttpAddress.isEmptyOrNull(HttpAddress.empty()));
        assertFalse(HttpAddress.isEmptyOrNull(HttpAddress.builder().scheme("https").build()));
    }

    @Test
    public void testReturnPresentOptionalForExistingQueryParameter() {
        final String queryParameterName = "parameter";
        final String queryParameterValue = "value";
        final HttpAddress httpAddress = HttpAddress.builder()
                .queryMap(singleQuery(queryParameterName, queryParameterValue))
                .build();
        final List<String> parameterValue =
                httpAddress.getQueryParameterValues(queryParameterName);

        assertThat(parameterValue, hasSize(1));
        assertEquals(queryParameterValue, parameterValue.get(0));
    }

    @Test
    public void testReturnAbsentOptionalForNonExistingQueryParameter() {
        final HttpAddress httpAddress = HttpAddress.empty();
        final List<String> parameterValue = httpAddress.getQueryParameterValues("parameter");
        assertThat(parameterValue, empty());
    }

    @Test
    public void testGetHostNotAlsoReturnsScheme() {
        final String host = "yandex.ru";
        final HttpAddress httpAddress = HttpAddress.builder()
                .scheme("https")
                .host(host)
                .build();
        assertEquals(host, httpAddress.getHost());
    }

    @Test
    public void testReturnPathSeparatorForEmptyPath() {
        final HttpAddress httpAddress = HttpAddress.empty();
        assertEquals(HttpAddress.PATH_SEPARATOR, httpAddress.getEncodedPath());
    }

    @Test
    public void testNonEmptyPathStartsWithPathSeparator() {
        final HttpAddress httpAddress = HttpAddress.builder()
                .pathSegments(Collections.singletonList("cart"))
                .build();
        assertThat(httpAddress.getEncodedPath(), startsWith(HttpAddress.PATH_SEPARATOR));
    }

    @Test
    public void testSkipSchemeSeparatorIfSchemeIsEmpty() {
        final HttpAddress httpAddress = HttpAddress.builder()
                .host("yandex.ru")
                .pathSegments(Collections.singletonList("cart"))
                .build();
        assertThat(httpAddress.getSchemeAndHost(), not(startsWith(HttpAddress.SCHEME_SEPARATOR)));
    }

    @Test
    public void testDisregardQueryParametersOrderDuringComparison() {
        final HttpAddress firstAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = HttpAddress.builder()
                .addQueryParameter("param2", "value2")
                .addQueryParameter("param1", "value1")
                .build();

        final HttpAddress thirdAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param3", "value3")
                .build();

        assertEquals(firstAddress, secondAddress);
        assertNotEquals(firstAddress, thirdAddress);
        assertNotEquals(secondAddress, thirdAddress);
    }

    @Test
    public void testReturnsAllDifferentQueryParameters() {

        final HttpAddress firstAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value12")
                .addQueryParameter("param3", "value3")
                .build();

        assertThat(firstAddress.getDifferentQueryParameterNames(secondAddress),
                containsInAnyOrder("param2", "param3"));
    }

    @Test
    public void testReturnsEmptyListIfAllQueryParametersAreEquals() {

        final HttpAddress firstAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        assertThat(firstAddress.getDifferentQueryParameterNames(secondAddress), empty());
    }

    @Test(expected = NullPointerException.class)
    public void testThrowsExceptionIfNullPassedToGetDifferentQueryParameterNamesMethod() {
        //noinspection ConstantConditions
        HttpAddress.empty().getDifferentQueryParameterNames(null);
    }

    @Test
    public void testHttpAddressStringRepresentationIsOk() {
        final HttpAddress httpAddress = HttpAddress.builder()
                .scheme("https")
                .host("yandex.ru")
                .addPathSegment("something")
                .addPathSegment("special")
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .fragment("fragment")
                .build();

        assertEquals("https://yandex.ru/something/special?param1=value1&param2=value2#fragment",
                httpAddress.asEncodedString());
    }

    @Test
    public void testRespectPathSegmentsOrderDuringComparison() {

        final HttpAddress firstHttpAddress = HttpAddress.builder()
                .addPathSegment("1")
                .addPathSegment("2")
                .build();

        final HttpAddress secondHttpAddress = HttpAddress.builder()
                .addPathSegment("2")
                .addPathSegment("1")
                .build();

        assertNotEquals(firstHttpAddress, secondHttpAddress);
    }

    @Test
    public void testGetEncodedPathResultIsOk() {
        final HttpAddress httpAddress = HttpAddress.builder()
                .addPathSegment("search")
                .addPathSegment("filters")
                .addPathSegment("12345")
                .build();

        assertEquals("/search/filters/12345", httpAddress.getEncodedPath());
    }

    @Test
    public void testAddingPatSegmentWithBuilderNotThrowingException() {
        HttpAddress.empty()
                .toBuilder()
                .addPathSegment("segment")
                .build();
    }

    @Test
    public void testAddingQueryParamWithBuilderNotThrowingException() {
        HttpAddress.empty()
                .toBuilder()
                .addQueryParameter("param", "value")
                .build();
    }

    @Test
    public void testToEncodedStringValueIsOk() {
        final HttpAddress httpAddress = HttpAddress.builder()
                .scheme("https")
                .host("yandex.ru")
                .addPathSegment("ran dom")
                .addPathSegment("pa rt")
                .addQueryParameter("pa ram", "va lue")
                .addQueryParameter("param2", "value2")
                .fragment("frag ment")
                .build();

        assertEquals(
                "https://yandex.ru/ran%20dom/pa%20rt?pa%20ram=va%20lue&param2=value2#frag%20ment",
                httpAddress.asEncodedString());
    }

    @Test
    public void testWithoutFragmentRemovesFragmentIfAny() {
        HttpAddress httpAddress = HttpAddress.builder()
                .scheme("https")
                .host("market.yandex.ru")
                .addPathSegment("product")
                .fragment("spec")
                .build();

        assertEquals("https://market.yandex.ru/product",
                httpAddress.withoutFragment().asEncodedString());

        httpAddress = HttpAddress.builder()
                .scheme("https")
                .host("market.yandex.ru")
                .addPathSegment("product")
                .build();

        assertSame(httpAddress, httpAddress.withoutFragment());
    }

    @Test
    public void testAddParamWithTheSameName() {
        final HttpAddress firstAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .addQueryParameter("param2", "value3")
                .build();

        assertNotEquals(firstAddress, secondAddress);
    }

    @Test
    public void testAddQueryParameterIfAbsentInList() {
        final HttpAddress firstAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .addQueryParameterIfAbsentInList("param2", "value3")
                .build();

        assertEquals(firstAddress, secondAddress);
    }

    @Test
    public void testReplaceQueryParameterIfExists() {
        final HttpAddress initialAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = initialAddress.toBuilder().replaceQueryParameter(
                "param1", "value3").build();

        assertEquals("param2=value2&param1=value3", secondAddress.getQuery(false));
    }

    @Test
    public void testReplaceQueryParameterIfNotExists() {
        final HttpAddress initialAddress = HttpAddress.builder()
                .addQueryParameter("param1", "value1")
                .addQueryParameter("param2", "value2")
                .build();

        final HttpAddress secondAddress = initialAddress.toBuilder().replaceQueryParameter(
                "param3", "value3").build();

        assertEquals("param1=value1&param2=value2&param3=value3", secondAddress.getQuery(false));
    }

    @NonNull
    private QueryMap singleQuery(@NonNull final String name, @NonNull final String value) {
        return QueryMap.builder()
                .add(name, value)
                .build();
    }
}