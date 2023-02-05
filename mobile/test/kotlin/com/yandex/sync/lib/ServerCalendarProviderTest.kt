package com.yandex.sync.lib

import biweekly.io.ICalTimeZone
import com.yandex.sync.lib.entity.CalendarEntity
import com.yandex.sync.lib.entity.EventEntity
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*


class ServerCalendarProviderTest {

    private lateinit var serverCalendarProvider: ServerCalendarProvider

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        val dispatcher = object : Dispatcher() {

            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.path) {
                    "/" -> MockResponses.PRINCIPAL
                    "/principals/users/thevery%40yandex-team.ru/" -> MockResponses.USERINFO
                    "/calendars/thevery%40yandex-team.ru/" -> MockResponses.CALENDARS
                    "/calendars/thevery%40yandex-team.ru/events-12618/" -> MockResponses.CALENDAR
                    "/calendars/ttqul%40yandex.ru/events-5632612/4aiyLmbLyandex.ru.ics" -> ""
                    "/calendars/ttqul%40yandex.ru/events-5632612/thhnxh8myandex.ru.ics" -> ""
                    "/calendars/ttqul%40yandex.ru/events-5632612/VzWIZQUmyandex.ru.ics" -> ""
                    else -> throw IllegalStateException("Unknown path: ${request.path}")
                }
                return MockResponse().setBody(body).setResponseCode(200)
            }
        }
        // Create a MockWebServer. These are lean enough that you can create a new
        // instance for every unit test.
        server = MockWebServer()
        server.setDispatcher(dispatcher)

        // Start the server.
        server.start()

        // Ask the server for its URL. You'll need this to make HTTP requests.
        val baseUrl = server.url("")

        serverCalendarProvider = ServerCalendarProvider(
                { Single.just("dummy") },
                SyncProperties(baseUrl = baseUrl.toString()),
                OkHttpClient()
        )

    }

    @After
    fun after() {
        server.shutdown()
    }

    @Test
    fun getContainers() {
        val calendars = serverCalendarProvider.getContainers().blockingGet()
        assertEquals(3, calendars.size)
        assertThat(calendars).containsExactlyInAnyOrder(
                CalendarEntity(
                        calendarId = -1L,
                        displayName = "Школа Мобильной разработки 2017",
                        name = "Школа Мобильной разработки 2017",
                        color = "#0d866aff",
                        owner = "thevery@yandex-team.ru",
                        href = "/calendars/thevery%40yandex-team.ru/events-38152/",
                        ctag = "1514287893608",
                        syncToken = "data:,1514287893608",
                        canWrite = true,
                        canRead = true
                ),
                CalendarEntity(
                        calendarId = -1L,
                        name = "Отсутствия",
                        displayName = "Отсутствия",
                        color = "#8f499eff",
                        owner = "thevery@yandex-team.ru",
                        href = "/calendars/thevery%40yandex-team.ru/events-12873/",
                        ctag = "1518313480268",
                        syncToken = "data:,1518313480268",
                        canWrite = false,
                        canRead = true
                ),
                CalendarEntity(
                        calendarId = -1L,
                        name = "Ильдар Каримов",
                        displayName = "Ильдар Каримов",
                        color = "#49c0a8ff",
                        owner = "thevery@yandex-team.ru",
                        href = "/calendars/thevery%40yandex-team.ru/events-12618/",
                        ctag = "1518307816957",
                        syncToken = "data:,1518307816957",
                        canWrite = false,
                        canRead = true
                )
        )
    }

    @Test
    fun getItems() {
        val calendars = serverCalendarProvider.getContainers().blockingGet()
        val calendarEntity = calendars.first()
        val events = serverCalendarProvider.getItems(calendarEntity.href, calendarEntity.syncToken).blockingGet().items
            .map {
                if (it.timeZone != null)
                    return@map it.copy(
                        timeZone = TimeZone.getTimeZone(it.timeZone?.toZoneId()),
                        endTimeZone = TimeZone.getTimeZone(it.endTimeZone?.toZoneId())
                    )
                else return@map it
            }
        assertEquals(5, events.size)

        assertThat(events).containsExactlyInAnyOrder(
                EventEntity(
                        id = -1L,
                        title = "123",
                        startTime = Date(1517258700000),
                        endTime = Date(1517260500000),
                        icsHref = "/calendars/ttqul%40yandex.ru/events-5632612/thhnxh8myandex.ru.ics",
                        uid = "thhnxh8myandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = "1517258265195",
                        url = "https://calendar.yandex.ru/event?event_id=490833420",
                        timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                        endTimeZone = TimeZone.getTimeZone("Europe/Moscow")
                ).apply {
                    responseStatus = EventEntity.STATUS_PRESENT
                },
                EventEntity(
                        id = -1L,
                        title = "tetwe",
                        startTime = Date(1540476900000),
                        endTime = Date(1540478700000),
                        icsHref = "/calendars/ttqul%40yandex.ru/events-5632612/uvow8ANsyandex.ru.ics",
                        uid = "uvow8ANsyandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = "1540395421512",
                        url = "https://calendar.yandex.ru/event?event_id=716561829",
                        timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                    endTimeZone = TimeZone.getTimeZone("Europe/Moscow")
                ).apply {
                    responseStatus = EventEntity.STATUS_PRESENT
                },
                EventEntity(
                        id = -1L,
                        title = "dffdfd",
                        startTime = Date(1540481400000),
                        endTime = Date(1540483200000),
                        icsHref = "/calendars/ttqul%40yandex.ru/events-5632612/4aiyLmbLyandex.ru.ics",
                        uid = "4aiyLmbLyandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = "1540474466373",
                        url = "https://calendar.yandex.ru/event?event_id=717702303" ,
                        timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                    endTimeZone = TimeZone.getTimeZone("Europe/Moscow")
                ).apply {
                    responseStatus = EventEntity.STATUS_PRESENT
                },
                EventEntity(
                        id = -1L,
                        title = "ttt",
                        startTime = Date(1540470600000),
                        endTime = Date(1540472400000),
                        icsHref = "/calendars/ttqul%40yandex.ru/events-5632612/VzWIZQUmyandex.ru.ics",
                        uid = "VzWIZQUmyandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = "1540389365361",
                        url = "https://calendar.yandex.ru/event?event_id=716559441",
                        timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                    endTimeZone = TimeZone.getTimeZone("Europe/Moscow")
                ).apply {
                    responseStatus = EventEntity.STATUS_PRESENT
                },
                EventEntity(
                        id = -1L,
                        title = "",
                        startTime = Date(0),
                        endTime = Date(0),
                        icsHref = "/calendars/ttqul%40yandex.ru/events-5632612/7AIVkX2eyandex.ru.ics",
                        uid = "",
                        etag = ""
                ).apply {
                    responseStatus = EventEntity.STATUS_REMOVED
                }
        )
    }

    @Test
    fun `diff upload`() {

        val icsHrefCreate = "/calendars/ttqul%40yandex.ru/events-5632612/thhnxh8myandex.ru.ics"
        val icsHrefDelete = "/calendars/ttqul%40yandex.ru/events-5632612/4aiyLmbLyandex.ru.ics"
        val icsHrefUpdate = "/calendars/ttqul%40yandex.ru/events-5632612/VzWIZQUmyandex.ru.ics"

        val calendar = CalendarEntity(
                calendarId = 1L,
                displayName = "Школа Мобильной разработки 2017",
                name = "Школа Мобильной разработки 2017",
                color = "#0d866aff",
                owner = "thevery@yandex-team.ru",
                href = "/calendars/thevery%40yandex-team.ru/events-38152/",
                ctag = "1514287893608",
                syncToken = "data:,1514287893608",
                canWrite = true,
                canRead = true
        )

        val updated = serverCalendarProvider.applyDiffEvents(calendar,
                listOf(EventEntity(
                        id = 1L,
                        title = "123",
                        startTime = Date(1517258700000),
                        endTime = Date(1517260500000),
                        icsHref = icsHrefCreate,
                        uid = "thhnxh8myandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = ""
                )),
                listOf(EventEntity(
                        id = 2L,
                        title = "ttt",
                        startTime = Date(1540470600000),
                        endTime = Date(1540472400000),
                        icsHref = icsHrefUpdate,
                        uid = "VzWIZQUmyandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = "1540389365361"
                )),
                listOf(EventEntity(
                        id = 3L,
                        title = "dffdfd",
                        startTime = Date(1540481400000),
                        endTime = Date(1540483200000),
                        icsHref = icsHrefDelete,
                        uid = "4aiyLmbLyandex.ru",
                        transparency = EventEntity.OPAQUE,
                        etag = "1540474466373"
                ))
        )

        val requests = listOf(server.takeRequest(), server.takeRequest(), server.takeRequest())

        assertThat(requests).areExactly(1, object : Condition<RecordedRequest>() {
            override fun matches(value: RecordedRequest): Boolean {
                return value.method == "DELETE"
                        && value.path == icsHrefDelete
            }
        })

        assertThat(requests).areExactly(1, object : Condition<RecordedRequest>() {
            override fun matches(value: RecordedRequest): Boolean {
                return value.method == "PUT"
                        && value.path == icsHrefCreate
                        && value.getHeader("If-Match") == null
            }
        })

        assertThat(requests).areExactly(1, object : Condition<RecordedRequest>() {
            override fun matches(value: RecordedRequest): Boolean {
                return value.method == "PUT"
                        && value.path == icsHrefUpdate
                        && value.getHeader("If-Match") == "1540389365361"
            }
        })

        assertEquals(2, updated.size)
        assertThat(updated).containsExactlyInAnyOrder(EventEntity(
                id = 1L,
                title = "123",
                startTime = Date(1517258700000),
                endTime = Date(1517260500000),
                icsHref = icsHrefCreate,
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                etag = ""
        ), EventEntity(
                id = 2L,
                title = "ttt",
                startTime = Date(1540470600000),
                endTime = Date(1540472400000),
                icsHref = icsHrefUpdate,
                uid = "VzWIZQUmyandex.ru",
                transparency = EventEntity.OPAQUE,
                etag = "1540389365361"
        ))
    }

    @Test
    fun `not crash when timeout`() {
        //TODO
    }

    @Test
    fun `not crash when data is broken`() {
        //TODO
    }
}