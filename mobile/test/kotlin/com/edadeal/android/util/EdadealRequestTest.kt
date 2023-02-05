package com.edadeal.android.util

import com.edadeal.android.dto.EdadealRequest
import com.edadeal.android.dto.EdadealRequest.Companion.isEdadealRequest
import com.squareup.moshi.Moshi
import okhttp3.Request
import org.hamcrest.Description
import org.hamcrest.MatcherAssert
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class EdadealRequestTest(
    private val url: String,
    private val expected: EdadealRequest?,
    private val expectedRequest: Request?,
    private val extraHeaders: Map<String, String>?,
) {

    private val moshi = Moshi.Builder().setupMoshi().build()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> = run {
            val url = "https://an.yandex.ru/meta/987081?imp-id=1&target-ref=https://com.edadeal.android/&charse" +
                "t=utf-8&callback=json&pcode-version=14640&mobile-ifa=ca8b4712-9a01-4310-a687-f46578d0a846&nanp" +
                "u-passport-id=293894892&device-id=24d4bc6d2c02f1f4442d2da9271a81e8&uuid=34cdb635e73c4c3ab2124f" +
                "7ec399bf66"
            listOf(
                arrayOf(
                    "edadeal-request:eyJtZXRob2QiOiJHRVQiLCJ1cmwiOiJodHRwczovL2FuLnlhbmRleC5ydS9tZXRhLzk4NzA4MT" +
                        "9pbXAtaWQ9MSZ0YXJnZXQtcmVmPWh0dHBzOi8vY29tLmVkYWRlYWwuYW5kcm9pZC8mY2hhcnNldD11dGYtOCZj" +
                        "YWxsYmFjaz1qc29uJnBjb2RlLXZlcnNpb249MTQ2NDAmbW9iaWxlLWlmYT1jYThiNDcxMi05YTAxLTQzMTAtYT" +
                        "Y4Ny1mNDY1NzhkMGE4NDYmbmFucHUtcGFzc3BvcnQtaWQ9MjkzODk0ODkyJmRldmljZS1pZD0yNGQ0YmM2ZDJj" +
                        "MDJmMWY0NDQyZDJkYTkyNzFhODFlOCZ1dWlkPTM0Y2RiNjM1ZTczYzRjM2FiMjEyNGY3ZWMzOTliZjY2IiwiaG" +
                        "VhZGVycyI6eyJ1c2VyLWFnZW50IjoiTW96aWxsYS81LjAgKExpbnV4OyBhcm1fNjQ7IEFuZHJvaWQgMTA7IE1J" +
                        "IDkpIENocm9tZS85Mi4wLjQ1MTUuMTY2In19",
                    EdadealRequest(
                        method = "GET",
                        url = url,
                        headers = mapOf(
                            "user-agent" to "Mozilla/5.0 (Linux; arm_64; Android 10; MI 9) Chrome/92.0.4515.166"
                        ),
                        body = null
                    ),
                    Request.Builder().url(url).get()
                        .header("user-agent", "Mozilla/5.0 (Linux; arm_64; Android 10; MI 9) Chrome/92.0.4515.166")
                        .build(),
                    mapOf("User-Agent" to "Edadeal/x.y.z")
                ),
                arrayOf(
                    "edadeal-request:eyJtZXRob2QiOiJHRVQiLCJ1cmwiOiJodHRwczovL2FuLnlhbmRleC5ydS9tZXRhLzk4NzA4MT" +
                        "9pbXAtaWQ9MSZ0YXJnZXQtcmVmPWh0dHBzOi8vY29tLmVkYWRlYWwuYW5kcm9pZC8mY2hhcnNldD11dGYtOCZj" +
                        "YWxsYmFjaz1qc29uJnBjb2RlLXZlcnNpb249MTQ2NDAmbW9iaWxlLWlmYT1jYThiNDcxMi05YTAxLTQzMTAtYT" +
                        "Y4Ny1mNDY1NzhkMGE4NDYmbmFucHUtcGFzc3BvcnQtaWQ9MjkzODk0ODkyJmRldmljZS1pZD0yNGQ0YmM2ZDJj" +
                        "MDJmMWY0NDQyZDJkYTkyNzFhODFlOCZ1dWlkPTM0Y2RiNjM1ZTczYzRjM2FiMjEyNGY3ZWMzOTliZjY2In0=",
                    EdadealRequest(
                        method = "GET",
                        url = url,
                        headers = null,
                        body = null
                    ),
                    Request.Builder().url(url).get()
                        .header("User-Agent", "Edadeal/x.y.z")
                        .build(),
                    mapOf("User-Agent" to "Edadeal/x.y.z")
                ),
                arrayOf(
                    "edadeal-request:eyJtZXRob2QiOiJIRUFEIiwidXJsIjoiaHR0cHM6Ly9hbi55YW5kZXgucnUvbWV0YS85ODcwOD" +
                        "E/aW1wLWlkPTEmdGFyZ2V0LXJlZj1odHRwczovL2NvbS5lZGFkZWFsLmFuZHJvaWQvJmNoYXJzZXQ9dXRmLTgm" +
                        "Y2FsbGJhY2s9anNvbiZwY29kZS12ZXJzaW9uPTE0NjQwJm1vYmlsZS1pZmE9Y2E4YjQ3MTItOWEwMS00MzEwLW" +
                        "E2ODctZjQ2NTc4ZDBhODQ2Jm5hbnB1LXBhc3Nwb3J0LWlkPTI5Mzg5NDg5MiZkZXZpY2UtaWQ9MjRkNGJjNmQy" +
                        "YzAyZjFmNDQ0MmQyZGE5MjcxYTgxZTgmdXVpZD0zNGNkYjYzNWU3M2M0YzNhYjIxMjRmN2VjMzk5YmY2NiJ9",
                    EdadealRequest(
                        method = "HEAD",
                        url = url,
                        headers = null,
                        body = null
                    ),
                    Request.Builder().url(url).head()
                        .build(),
                    null
                ),
                arrayOf("https://ads.edadeal.ru/v1/telemetry", null, null, null)
            )
        }
    }

    @Test
    fun `should return expected edadeal request`() {
        val request = when (isEdadealRequest(url)) {
            true -> EdadealRequest.from(moshi, url)
            else -> null
        }
        assertEquals(expected, request)
    }

    @Test
    fun `should build expected http request`() {
        val httpRequest = when (isEdadealRequest(url)) {
            true -> EdadealRequest.from(moshi, url).build(extraHeaders)
            else -> null
        }
        when (expectedRequest) {
            null -> assertNull(httpRequest)
            else -> MatcherAssert.assertThat(httpRequest, RequestMatcher(expectedRequest))
        }
    }

    private class RequestMatcher(
        private val request: Request
    ) : TypeSafeMatcher<Request>() {

        override fun describeTo(description: Description) {
            describe(description, request)
        }

        override fun matchesSafely(item: Request): Boolean {
            return request.url() == item.url() &&
                request.method() == item.method() &&
                request.headers() == item.headers() &&
                isRequestBodyMatchesSafely(item)
        }

        override fun describeMismatchSafely(
            item: Request,
            mismatchDescription: Description
        ) {
            describe(mismatchDescription, item)
        }

        private fun isRequestBodyMatchesSafely(item: Request): Boolean {
            val requestBody = request.body()
            val itemRequestBody = item.body()
            return when {
                requestBody == null && itemRequestBody == null -> true
                requestBody != null && itemRequestBody != null ->
                    requestBody.contentType() == itemRequestBody.contentType() &&
                    requestBody.contentLength() == itemRequestBody.contentLength()
                else -> false
            }
        }

        private fun describe(
            description: Description,
            request: Request
        ) {
            with(description) {
                appendText("{")
                appendText("method=${request.method()}")
                appendText(", url=${request.url()}")
                appendText(", headers=[")
                for (i in 0 until request.headers().size()) {
                    appendText("${request.headers().name(i)}: ${request.headers().value(i)}")
                    if (i < request.headers().size() - 1) {
                        appendText(", ")
                    }
                }
                appendText("], body={")
                request.body()?.let { body ->
                    appendText("contentType=")
                    appendText(body.contentType().toString())
                    appendText(", contentLength=")
                    appendText(body.contentLength().toString())
                }
                appendText("}")
            }
        }
    }
}
