package com.yandex.xplat.common

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultUriTests {
    @Test
    fun shouldBuildFileUri() {
        val uri = Uris.fromFilePath("/path/to/file")
        assertEquals(uri.getAbsoluteString(), "file:///path/to/file")
        assertEquals(uri.getScheme(), "file")
        assertNull(uri.getHost())
        assertEquals(uri.getPath(), "/path/to/file")
        assertEquals(uri.getPathSegments(), listOf("path", "to", "file"))
        assertNull(uri.getQuery())
        assertEquals(uri.getAllQueryParameters(), listOf<UriQueryParameter>())
        assertNull(uri.getFragment())
        assertTrue(uri.isFileUri())
    }

    @Test
    fun shouldBuildWebUri() {
        val uri = Uris.fromString("https://ya.ru/path/to/resource?param=value&foo=bar#fragment")!!
        assertEquals(
            uri.getAbsoluteString(),
            "https://ya.ru/path/to/resource?param=value&foo=bar#fragment"
        )
        assertEquals(uri.getScheme(), "https")
        assertEquals(uri.getHost(), "ya.ru")
        assertEquals(uri.getPath(), "/path/to/resource")
        assertEquals(uri.getPathSegments(), listOf("path", "to", "resource"))
        assertEquals(uri.getQuery(), "param=value&foo=bar")
        assertEquals(
            uri.getAllQueryParameters().map { it.name to it.value },
            listOf("param" to "value", "foo" to "bar")
        )
        assertEquals(uri.getFragment(), "fragment")
        assertFalse(uri.isFileUri())
    }

    @Test
    fun shouldBuildFileUriFromString() {
        val uri = Uris.fromString("file:///path/to/file")!!
        assertEquals(uri.getPath(), "/path/to/file")
        assertEquals(uri.getPathSegments(), listOf("path", "to", "file"))
        assertTrue(uri.isFileUri())
    }

    @Test
    fun shouldFailToBuildWebUriIfTheValueIsInvalid() {
        val uri = Uris.fromString("invalid value")
        assertNull(uri)
    }

    @Test
    fun shouldChangeUri() {
        val builder =
            Uris.fromString("https://ya.ru/path/to/resource?param=value&foo=bar#fragment")!!
                .builder()
        assertEquals(
            builder.setScheme("http").build().getAbsoluteString(),
            "http://ya.ru/path/to/resource?param=value&foo=bar#fragment"
        )
        assertEquals(
            builder.setHost("yandex.ru").build().getAbsoluteString(),
            "http://yandex.ru/path/to/resource?param=value&foo=bar#fragment"
        )
        assertEquals(
            builder.setPath("path/to/new-resource").build().getAbsoluteString(),
            "http://yandex.ru/path/to/new-resource?param=value&foo=bar#fragment"
        )
        assertEquals(
            builder.setPath("/path/to/yet-another-resource").build().getAbsoluteString(),
            "http://yandex.ru/path/to/yet-another-resource?param=value&foo=bar#fragment"
        )
        assertEquals(
            builder.setAllQueryParameters(
                mutableListOf(
                    UriQueryParameter(
                        "new_param",
                        "value"
                    ), UriQueryParameter("bar", "bazz")
                )
            ).build().getAbsoluteString(),
            "http://yandex.ru/path/to/yet-another-resource?new_param=value&bar=bazz#fragment"
        )
        assertEquals(
            builder.setAllQueryParameters(mutableListOf()).build().getAbsoluteString(),
            "http://yandex.ru/path/to/yet-another-resource#fragment"
        )
        assertEquals(
            builder.setFragment("new-fragment").build().getAbsoluteString(),
            "http://yandex.ru/path/to/yet-another-resource#new-fragment"
        )
        assertEquals(
            builder.setFragment("").build().getAbsoluteString(),
            "http://yandex.ru/path/to/yet-another-resource"
        )
        assertEquals(builder.setPath("").build().getAbsoluteString(), "http://yandex.ru/")
    }

    @Test
    fun uriQueryParameters() {
        var uri = Uris.fromString("https://ya.ru")!!
        assertEquals(uri.getAllQueryParameters(), listOf<UriQueryParameter>())
        assertNull(uri.getQueryParameter("foo"))
        assertEquals(uri.getQueryParameters("foo"), listOf<String>())
        assertEquals(uri.getQueryParameterNames(), listOf<String>())

        uri = uri.builder()
            .appendQueryParameter("foo", "bar")
            .appendQueryParameter("param", "val1")
            .appendQueryParameter("param", "val2")
            .build()
        assertEquals(uri.getAbsoluteString(), "https://ya.ru?foo=bar&param=val1&param=val2")
        assertEquals(
            uri.getAllQueryParameters().map { it.name to it.value },
            listOf("foo" to "bar", "param" to "val1", "param" to "val2")
        )
        assertEquals(uri.getQueryParameter("foo"), "bar")
        assertEquals(uri.getQueryParameters("foo"), listOf("bar"))
        assertEquals(uri.getQueryParameter("param"), "val1")
        assertEquals(uri.getQueryParameters("param"), listOf("val1", "val2"))
        assertEquals(uri.getQueryParameterNames(), listOf("foo", "param"))

        uri = uri.builder().clearQuery().build()
        assertEquals(uri.getAbsoluteString(), "https://ya.ru")
        assertEquals(uri.getQueryParameterNames(), listOf<String>())

        uri = uri.builder().setAllQueryParameters(mutableListOf(UriQueryParameter("bar", "bazz")))
            .build()
        assertEquals(uri.getAbsoluteString(), "https://ya.ru?bar=bazz")
        assertEquals(uri.getQueryParameterNames(), listOf("bar"))

        uri = Uris.fromString("https://ya.ru?foo")!!
        assertEquals(uri.getAllQueryParameters().map { it.name to it.value }, listOf("foo" to ""))
        assertEquals(uri.getQueryParameter("foo"), "")
        assertEquals(uri.getQueryParameters("foo"), listOf(""))
        assertEquals(uri.getQueryParameterNames(), listOf("foo"))
    }

    @Test
    fun shouldEncode() {
        assertEquals(percentEncode("ABC", false), "ABC")
        assertEquals(percentEncode("АБВ", false), "%D0%90%D0%91%D0%92")

        assertEquals(percentEncode("ABC+abc 123", false), "ABC%2Babc%20123")
        assertEquals(percentEncode("ABC+abc 123", true), "ABC%2Babc+123")
    }

    @Test
    fun shouldDecode() {
        assertEquals(percentDecode("ABC", false), "ABC")
        assertEquals(percentDecode("%D0%90%D0%91%D0%92", false), "АБВ")

        assertEquals(percentDecode("ABC%2Babc%20123+456", false), "ABC+abc 123+456")
        assertEquals(percentDecode("ABC%2Babc%20123+456", true), "ABC+abc 123 456")
    }
}
