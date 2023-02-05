/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.http

import io.ktor.http.*
import io.ktor.util.*
import kotlin.test.*
import java.net.*

internal class URLBuilderTest {
    private val urlString = "http://localhost:8080/path"

    @Test
    fun takeFromURITest() {
        val url = URLBuilder().apply {
            takeFrom(URI.create(urlString))
        }

        with(url.build()) {
            assertEquals("localhost", host)
            assertEquals("/path", fullPath)
            assertEquals(8080, port)
        }
    }

    @Test
    fun buildStringTest() {
        val url = URLBuilder().apply {
            takeFrom(URI.create(urlString))
        }

        assertEquals(urlString, url.buildString())
    }

    @Test
    fun defaultPortBuildStringTest() {
        val url = URLBuilder().apply {
            takeFrom(URI.create("http://localhost:80/path"))
        }

        assertEquals("http://localhost/path", url.buildString())
    }

    @Test
    fun testEmptyPath() {
        val urlStr1 = "http://localhost"
        val urlStr2 = "http://localhost?param1=foo"

        val url1 = URLBuilder().apply {
            takeFrom(URI.create(urlStr1))
        }
        val url2 = URLBuilder().apply {
            takeFrom(URI.create(urlStr2))
        }

        assertEquals(urlStr1, url1.buildString())
        assertEquals(urlStr2, url2.buildString())
    }

    @Test
    fun testCustom() {
        val url = URLBuilder().apply {
            takeFrom(URI.create("custom://localhost:8080/path"))
        }

        assertEquals("custom://localhost:8080/path", url.buildString())
    }

    @Test
    fun testWss() {
        val url = URLBuilder().apply {
            takeFrom(URI.create("wss://localhost/path"))
        }

        assertEquals("wss://localhost/path", url.buildString())
        assertEquals(443, url.port)
    }

    @Test
    fun testCapitalize() {
        val url = URLBuilder().apply {
            takeFrom(URI.create("custom://localhost:8080/path".capitalize()))
        }

        assertEquals("custom://localhost:8080/path", url.buildString())
    }

    @Test
    fun testTakeFromRewritePort() {
        URLBuilder().apply {
            port = 9093
            takeFrom(URI("http://localhost:81/"))
        }.buildString().let { url -> assertEquals("http://localhost:81/", url) }

        URLBuilder().apply {
            port = 9093
            takeFrom(URI("http://localhost/"))
        }.buildString().let { url -> assertEquals("http://localhost/", url) }

        URLBuilder().apply {
            port = 9093
            takeFrom(URI("/test"))
        }.buildString().let { url -> assertEquals("http://localhost:9093/test", url) }
    }

    @Test
    fun testParseSchemeWithDigits() {
        testBuildString("a123://google.com")
    }

    @Test
    fun testParseSchemeWithDotsPlusAndMinusSigns() {
        testBuildString("a.+-://google.com")
    }

    @Test
    fun testParseSchemeWithCapitalCharacters() {
        testBuildString("HTTP://google.com")
    }

    @Test
    fun testParseSchemeNotStartedWithLetter() {
        for (index in 0..0x7F) {
            val char = index.toChar()

            if (char in 'a'..'z' || char in 'A'..'Z') {
                testBuildString("${char}http://google.com")
            } else {
                assertFails("Character $char is not allowed at the first position in the scheme.") {
                    testBuildString("${char}http://google.com")
                }
            }
        }
    }

    @Test
    fun portIsNotInStringIfItMatchesTheProtocolDefaultPort() {
        URLBuilder().apply {
            protocol = URLProtocol("custom", 12345)
            port = 12345
        }.buildString().let {
            assertEquals("custom://localhost/", it)
        }
    }

    @Test
    fun settingTheProtocolDoesNotOverwriteAnExplicitPort() {
        URLBuilder().apply {
            port = 8080
            protocol = URLProtocol.HTTPS
        }.buildString().let { url ->
            assertEquals("https://localhost:8080/", url)
        }
    }

    @Test
    fun protocolDefaultPortIsUsedIfAPortIsNotSpecified() {
        URLBuilder().apply {
            protocol = URLProtocol.HTTPS

            assertEquals(DEFAULT_PORT, port)
        }.build().also { url ->
            assertEquals(URLProtocol.HTTPS.defaultPort, url.port)
        }
    }

    @Test
    fun anExplicitPortIsUsedIfSpecified() {
        URLBuilder().apply {
            protocol = URLProtocol.HTTPS
            port = 2048

            assertEquals(2048, port)
        }.build().also { url ->
            assertEquals(2048, url.port)
        }
    }

    @Test
    fun takeFromACustomProtocolAndSettingTheDefaultPort() {
        URLBuilder().apply {
            takeFrom("custom://localhost/path")
            protocol = URLProtocol("custom", 8080)

            assertEquals(DEFAULT_PORT, port)
        }.buildString().also { url ->
            // ensure that the built url does not specify the port when configuring the default port
            assertEquals("custom://localhost/path", url)
        }
    }

    @Test
    fun rewritePathWhenRewriteUrl() {
        val url = URLBuilder("https://httpstat.us/301")
        url.takeFrom("https://httpstats.us")

        assertEquals("", url.encodedPath)
    }

    @Test
    fun rewritePathFromSlash() {
        val url = URLBuilder("https://httpstat.us/301")

        url.takeFrom("/")
        assertEquals("https://httpstat.us/", url.buildString())

    }

    @Test
    fun rewritePathFromSingle() {
        val url = URLBuilder("https://httpstat.us/301")

        url.takeFrom("/1")
        assertEquals("https://httpstat.us/1", url.buildString())
    }

    @Test
    fun rewritePathDirectoryWithRelative() {
        val url = URLBuilder("https://example.org/first/directory/")

        url.takeFrom("relative")
        assertEquals("https://example.org/first/directory/relative", url.buildString())
    }

    @Test
    fun rewritePathFileWithRelative() {
        val url = URLBuilder("https://example.org/first/file.html")

        url.takeFrom("relative")
        assertEquals("https://example.org/first/relative", url.buildString())
    }

    @Test
    fun rewritePathFileWithDot() {
        val url = URLBuilder("https://example.org/first/file.html")

        url.takeFrom("./")
        assertEquals("https://example.org/first/./", url.buildString())
    }

    @Test
    fun queryParamsWithNoValue() {
        val url = URLBuilder("https://httpstat.us/?novalue")
        assertEquals("https://httpstat.us/?novalue", url.buildString())
    }

    @Test
    fun queryParamsWithEmptyValue() {
        val url = URLBuilder("https://httpstat.us/?empty=")
        assertEquals("https://httpstat.us/?empty=", url.buildString())
    }

    @Test
    fun emptyProtocolWithPort() {
        val url = URLBuilder("//whatever:8080/abc")

        assertEquals(URLProtocol.HTTP, url.protocol)
        assertEquals("whatever", url.host)
        assertEquals(8080, url.port)
        assertEquals("/abc", url.encodedPath)
    }

    @Test
    fun retainEmptyPath() {
        val url = URLBuilder("http://www.test.com")
        assertEquals("", url.encodedPath)
    }

    /**
     * Checks that the given [url] and the result of [URLBuilder.buildString] is equal (case insensitive).
     */
    private fun testBuildString(url: String) {
        assertEquals(url.toLowerCase(), URLBuilder(url).buildString().toLowerCase())
    }
}
