package io.ktor.tests.http

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.tests.*
import kotlin.test.*

class HeadersTest {

    @Test
    fun headersReturnNullWhenEmpty(): Unit = withTestApplication {
        application.routing {
            get("/") {
                assertNull(call.request.headers["X-Nonexistent-Header"])
                assertNull(call.request.headers.getAll("X-Nonexistent-Header"))

                call.respond(HttpStatusCode.OK, "OK")
            }
        }

        handleRequest(HttpMethod.Get, "/").let { call ->
            assertEquals(HttpStatusCode.OK, call.response.status())
            assertEquals("OK", call.response.content)
        }
    }
}
