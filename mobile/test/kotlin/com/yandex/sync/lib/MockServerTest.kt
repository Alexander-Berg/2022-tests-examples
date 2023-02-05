package com.yandex.sync.lib

import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert
import org.junit.Test

class MockServerTest {
    @Test
    fun `addition is correct`() {
        val dispatcher = object : Dispatcher() {

            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.path) {
                    "/" -> MockResponses.PRINCIPAL
                    "/principals/users/thevery%40yandex-team.ru/" -> MockResponses.USERINFO
                    "/calendars/thevery%40yandex-team.ru/" -> MockResponses.CALENDARS
                    "/calendars/thevery%40yandex-team.ru/events-12618/" -> MockResponses.CALENDAR
                    else -> throw IllegalStateException("Unknown path: ${request.path}")
                }
                return MockResponse().setBody(body).setResponseCode(200)
            }
        }
        // Create a MockWebServer. These are lean enough that you can create a new
        // instance for every unit test.
        val server = MockWebServer()
        server.setDispatcher(dispatcher)


        // Start the server.
        server.start()

        // Ask the server for its URL. You'll need this to make HTTP requests.
        val baseUrl = server.url("")

        val syncModel =
            ServerCalendarProvider(
                { Single.just("dummy") },
                SyncProperties(baseUrl = baseUrl.toString()),
                OkHttpClient()
            )
        syncModel.getContainers()

        // Exercise your application code, which should make those HTTP requests.
        // Responses are returned in the same order that they are enqueued.
        /*val chat = Chat(baseUrl)

        chat.loadMore()
        assertEquals("hello, world!", chat.messages())

        chat.loadMore()
        chat.loadMore()
        assertEquals(
            ""
                    + "hello, world!\n"
                    + "sup, bra?\n"
                    + "yo dog", chat.messages()
        )

        // Optional: confirm that your app made the HTTP requests you were expecting.
        val request1 = server.takeRequest()
        assertEquals("/v1/chat/messages/", request1.getPath())
        assertNotNull(request1.getHeader("Authorization"))

        val request2 = server.takeRequest()
        assertEquals("/v1/chat/messages/2", request2.getPath())

        val request3 = server.takeRequest()
        assertEquals("/v1/chat/messages/3", request3.getPath())

        // Shut down the server. Instances cannot be reused.
        server.shutdown()
*/

        Assert.assertEquals(1, 2 - 1)
    }
}
