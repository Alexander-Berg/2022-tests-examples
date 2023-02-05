package com.yandex.sync.lib.provider

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentProviderResult
import android.content.SyncResult
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Color
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import com.yandex.sync.lib.asSyncAdapter
import com.yandex.sync.lib.entity.CalendarEntity
import com.yandex.sync.lib.entity.EventEntity
import com.yandex.sync.lib.factory.AlarmEntityFactory
import com.yandex.sync.lib.factory.AttendeeEntityFactory
import com.yandex.sync.lib.factory.CalendarEntityFactory.CALENDAR_PROJECTION
import com.yandex.sync.lib.factory.EventEntityFactory.EVENT_PROJECTION
import com.yandex.sync.lib.utils.SyncTestRunner
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentProviderClient
import java.util.*


@RunWith(SyncTestRunner::class)
@Config(shadows = [ShadowContentProviderClient::class])
class LocalCalendarProviderTest {

    lateinit var provider: LocalCalendarProvider
    lateinit var resolver: ContentProviderClient
    lateinit var syncResult: SyncResult
    var account = Account("name", "type")

    private val calendarsCursor = createMockCalendarCursor()

    @Before
    fun setup() {
        syncResult = SyncResult()

        resolver = mockkClass(
                type = ContentProviderClient::class,
                relaxed = true,
                moreInterfaces = *arrayOf(AutoCloseable::class)
        ) {

            every {
                applyBatch(any())
            } returns arrayOf(mockkClass(ContentProviderResult::class))


            //For Getting Containers
            every {
                query(
                        Calendars.CONTENT_URI.asSyncAdapter(account),
                        CALENDAR_PROJECTION,
                        null,
                        null,
                        null
                )
            } returns calendarsCursor


            every {
                query(
                        CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                        EVENT_PROJECTION,
                        any(),
                        arrayOf("icsHref2"),
                        null
                )
            } returns createMockEvent2Cursor()

            every {
                query(
                        CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                        EVENT_PROJECTION,
                        any(),
                        arrayOf(SYNC_ID_1),
                        null
                )
            } returns createMockEvent1Cursor()

            every {
                query(
                        CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                        EVENT_PROJECTION,
                        any(),
                        arrayOf(SYNC_ID_NEW, SYNC_ID_1),
                        null
                )
            } returns createMockEventsAddCursor()

            every {
                query(
                        CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                        EVENT_PROJECTION,
                        any(),
                        arrayOf(SYNC_ID_NEW, SYNC_ID_1, SYNC_ID_2),
                        null
                )
            } returns createMockEventsAllCursor()

        }

        provider = LocalCalendarProvider(resolver, account, syncResult)
    }

    @Test
    fun `should extract calendar`() {
        val calendars = provider.getContainers().blockingGet()
        assertThat(calendars).hasSize(2)
        val calendar1 = calendars[0]

        assertThat(calendar1.calendarId).isEqualTo(1)
        assertThat(calendar1.color).isEqualTo("#ffaabbcc")
        assertThat(calendar1.displayName).isEqualTo("display_name")
        assertThat(calendar1.name).isEqualTo("name")
        assertThat(calendar1.syncId).isEqualTo(SYNC_ID_1)
        assertThat(calendar1.href).isEqualTo(calendar1.syncId)
        assertThat(calendar1.ctag).isEqualTo("ctag")

        val calendar2 = calendars[1]

        assertThat(calendar2.calendarId).isEqualTo(2)
        assertThat(calendar2.color).isEqualTo("#ffffffff")
        assertThat(calendar2.displayName).isEqualTo("display_name")
        assertThat(calendar2.name).isEqualTo("name")
        assertThat(calendar2.syncId).isEqualTo(SYNC_ID_2)
        assertThat(calendar2.href).isEqualTo(calendar2.syncId)
        assertThat(calendar2.ctag).isEqualTo("ctag")
    }

    @Test
    fun `should get correct calendar Id`() {
        val calendar = provider.getCalendarBySyncId(SYNC_ID_1)

        assertEquals(1L, calendar?.calendarId)

        val calendarId2 = provider.getCalendarBySyncId(SYNC_ID_2)?.calendarId

        assertEquals(2L, calendarId2)

        val calendarNotExists = provider.getCalendarBySyncId(SYNC_ID_1 + "test")

        assertEquals(null, calendarNotExists)
    }

    @Test
    fun `check apply dif calendar`() {
        val created = listOf(
                CalendarEntity(
                        calendarId = -1L,
                        displayName = "Школа Мобильной разработки 2017",
                        name = "Школа Мобильной разработки 2017",
                        color = "#0d866aff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "/calendars/thevery%40yandex-team.ru/events-38152/",
                        ctag = "1514287893608",
                        syncToken = "data:,1514287893608"
                )
        )
        val updated = listOf(
                CalendarEntity(
                        calendarId = 10L,
                        name = "Отсутствия",
                        displayName = "Отсутствия",
                        color = "#8f499eff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "/calendars/thevery%40yandex-team.ru/events-12873/",
                        ctag = "1518313480268",
                        syncToken = "data:,1518313480268"
                )
        )
        val deleted = listOf(
                CalendarEntity(
                        calendarId = 20L,
                        name = "Ильдар Каримов",
                        displayName = "Ильдар Каримов",
                        color = "#49c0a8ff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "/calendars/thevery%40yandex-team.ru/events-12618/",
                        ctag = "1518307816957",
                        syncToken = "data:,1518307816957"
                )
        )
        provider.applyDiffCalendar(
                created,
                updated,
                deleted
        )

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.filter { operation ->
                    operation.isInsert
                            && operation.uri.toString() == "content://com.android.calendar/calendars?caller_is_syncadapter=true&account_name=name&account_type=type"
                }.size == 1
            })
        }

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.filter { operation ->
                    operation.isUpdate
                            && operation.uri.toString() == "content://com.android.calendar/calendars/10?caller_is_syncadapter=true&account_name=name&account_type=type"
                }.size == 1
            })
        }

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.filter { operation ->
                    operation.isDelete
                            && operation.uri.toString() == "content://com.android.calendar/calendars/20?caller_is_syncadapter=true&account_name=name&account_type=type"
                }.size == 1
            })
        }
    }

    @Test
    fun `check get changed items`() {

        val calendarEmpty = CalendarEntity(
                calendarId = 101L,
                displayName = "Школа Мобильной разработки 2017",
                name = "Школа Мобильной разработки 2017",
                color = "#0d866aff",
                owner = "/principals/users/thevery%40yandex-team.ru/",
                href = "/calendars/thevery%40yandex-team.ru/events-38152/",
                ctag = "1514287893608",
                syncToken = "data:,1514287893608"
        )

        val calendar = CalendarEntity(
                calendarId = 100L,
                displayName = "Школа Мобильной разработки 2017",
                name = "Школа Мобильной разработки 2017",
                color = "#0d866aff",
                owner = "/principals/users/thevery%40yandex-team.ru/",
                href = "/calendars/thevery%40yandex-team.ru/events-38152/",
                ctag = "1514287893608",
                syncToken = "data:,1514287893608"
        )

        val selection = CalendarContract.Events.CALENDAR_ID + "=" +
                calendar.calendarId.toString() + " AND " + "(" +
                CalendarContract.Events.DIRTY + "=" + 1 + " OR " +
                CalendarContract.Events.DELETED + "=" + 1 + ")"

        every {
            resolver.query(
                    CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                    EVENT_PROJECTION,
                    selection,
                    null,
                    null
            )
        } returns createMockEventsChangedCursor()

        val eventsChangedEmpty = provider.getItems(calendarEmpty).blockingGet()
        assertEquals(0, eventsChangedEmpty.size)
        val eventsChanged = provider.getItems(calendar).blockingGet()
        assertEquals(2, eventsChanged.size)
        assertEquals(2, eventsChanged.filter { it.dirty || it.isDeleted }.size)
        assertEquals(1, eventsChanged.filter { it.isDeleted }.size)
        assertEquals(1, eventsChanged.filter { it.dirty }.size)
    }

    @Test
    fun `gatherEvent with parent event test`() {

        val selection = CalendarContract.Events.ORIGINAL_ID + "= ? OR " + CalendarContract.Events._ID + "= ? OR " + CalendarContract.Events._SYNC_ID + "= ?"

        val selectionAlarmAndAttendee = "(event_id IN (?,?))"

        every {
            resolver.query(
                    CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                    EVENT_PROJECTION,
                    selection,
                    arrayOf("4", "4", ""),
                    null
            )
        } returns createMockEventsOriginalAndExceptedCursor()

        every {
            resolver.query(
                    CalendarContract.Reminders.CONTENT_URI.asSyncAdapter(account),
                    AlarmEntityFactory.ALARM_PROJECTION,
                    selectionAlarmAndAttendee,
                    arrayOf("4", "6"),
                    null
            )
        } returns createMockAlarmsCursor()

        every {
            resolver.query(
                    CalendarContract.Attendees.CONTENT_URI.asSyncAdapter(account),
                    AttendeeEntityFactory.ATTENDEE_PROJECTION,
                    selectionAlarmAndAttendee,
                    arrayOf("4", "6"),
                    null
            )
        } returns createMockAttendeeCursor()

        val eventEntityParent = EventEntity(
                id = 4,
                title = "title",
                uid = "",
                etag = "",
                icsHref = "",
                startTime = Date(1541768799000),
                endTime = Date(1541768800000)
        )

        val eventEntityChild = EventEntity(
                id = 6,
                title = "title",
                uid = "",
                etag = "",
                icsHref = "",
                startTime = Date(1541768799000),
                endTime = Date(1541768800000),
                originalId = 4
        )

        val gatherEvent = provider.gatherEvent(eventEntityParent)
        val gatherEventChild = provider.gatherEvent(eventEntityChild)
        assertEquals(gatherEvent, gatherEventChild)

        assertEquals(4, gatherEvent.id)
        assertEquals(1, gatherEvent.exceptionDates.size)
        assertEquals(1, gatherEvent.alarms.size)
        assertEquals(10, gatherEvent.alarms.first().minBeforeEvent)
        assertEquals(1, gatherEvent.attendees.size)
        assertEquals("Your Name", gatherEvent.attendees.first().name)

        assertEquals(1, gatherEvent.exceptionDates.first().alarms.size)
        assertEquals(1, gatherEvent.exceptionDates.first().attendees.size)
        assertEquals("Your Child", gatherEvent.exceptionDates.first().attendees.first().name)
        assertEquals(20, gatherEvent.exceptionDates.first().alarms.first().minBeforeEvent)
        assertEquals(6, gatherEvent.exceptionDates.first().id)
    }

    @Test
    fun `check apply dif events without exDates, alarms and attendees`() {
        val created = listOf(
                EventEntity(
                        id = -1L,
                        title = "Школа Мобильной разработки 2017",
                        icsHref = "/principals/users/thevery%40yandex-team.ru/",
                        startTime = Date(1573516800),
                        endTime = Date(1573517800),
                        uid = SYNC_ID_NEW,
                        transparency = EventEntity.OPAQUE,
                        url = "url",
                        etag = ETAG_SYNC_ID_NEW
                )
        )
        val updated = listOf(
                EventEntity(
                        id = 1L,
                        title = "Updated",
                        icsHref = "icsHref1",
                        startTime = Date(1573516800),
                        endTime = Date(1573517800),
                        uid = SYNC_ID_1,
                        transparency = EventEntity.OPAQUE,
                        url = "url",
                        etag = ETAG_SYNC_ID_1
                )
        )
        val deleted = listOf(
                EventEntity(
                        id = 2L,
                        title = "",
                        icsHref = "icsHref2",
                        startTime = Date(0),
                        endTime = Date(0),
                        uid = SYNC_ID_2,
                        transparency = EventEntity.OPAQUE,
                        url = "url",
                        etag = ETAG_SYNC_ID_2
                )
        )
        provider.applyDiffEvents(
                CalendarEntity(
                        calendarId = 1,
                        name = "Ильдар Каримов",
                        displayName = "Ильдар Каримов",
                        color = "#49c0a8ff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "/calendars/thevery%40yandex-team.ru/events-12618/",
                        ctag = "1518307816957",
                        syncToken = "data:,1518307816957"
                ),
                created,
                updated,
                deleted
        )

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.filter { operation ->
                    operation.isInsert
                            && operation.uri.toString() == "content://com.android.calendar/events?caller_is_syncadapter=true&account_name=name&account_type=type"
                }.size == 1
            })
        }

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.filter { operation ->
                    operation.isUpdate
                            && operation.uri.toString() == "content://com.android.calendar/events/1?caller_is_syncadapter=true&account_name=name&account_type=type"
                }.size == 1
            })
        }

        //Deleting by syncId, not by id
        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.filter { operation ->
                    operation.isDelete
                            && operation.uri.toString() == "content://com.android.calendar/events?caller_is_syncadapter=true&account_name=name&account_type=type"
                }.size == 1
            })
        }

        verify(exactly = 0) {
            resolver.applyBatch(match {
                return@match it.any { operation ->
                    (operation.isInsert || operation.isUpdate) &&
                            operation.uri.toString().startsWith("content://com.android.calendar/reminders")
                }
            })
        }

        verify(exactly = 0) {
            resolver.applyBatch(match {
                return@match it.any { operation ->
                    (operation.isInsert || operation.isUpdate) &&
                            operation.uri.toString().startsWith("content://com.android.calendar/attendees")
                }
            })
        }

        verify(exactly = 0) {
            resolver.applyBatch(match {
                return@match it.any { operation ->
                    (operation.isInsert || operation.isUpdate) &&
                            operation.uri.toString().startsWith("content://com.android.calendar/exception")
                }
            })
        }

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.count { operation ->
                    operation.isDelete &&
                            operation.uri.toString().startsWith("content://com.android.calendar/reminders")
                } == 2
            })
        }

        verify(exactly = 1) {
            resolver.applyBatch(match {
                return@match it.count { operation ->
                    operation.isDelete &&
                            operation.uri.toString().startsWith("content://com.android.calendar/attendees")
                } == 2
            })
        }
    }

    companion object {
        const val SYNC_ID_1 = "_sync_id_1"
        const val SYNC_ID_2 = "_sync_id_2"
        const val SYNC_ID_NEW = "SYNC_ID_NEW"

        const val ETAG_SYNC_ID_1 = "ETAG_SYNC_ID_1"
        const val ETAG_SYNC_ID_2 = "ETAG_SYNC_ID_2"
        const val ETAG_SYNC_ID_NEW = "ETAG_SYNC_ID_NEW"

        private val EVENT1_FIELDS = arrayOf(
                "1", //Events._ID
                "title", //Events.TITLE
                "1541768799000", //Events.DTSTART
                "1541768800000",
                SYNC_ID_1,
                0, //Events.DIRTY
                0, //Events.DIRTY
                ETAG_SYNC_ID_1,
                "uid1",
                SYNC_ID_1,
                "", //ORIGINAL_SYNC_ID
                0, //ALL_DAY
                "", //RRULW
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )

        private val EVENT2_FIELDS = arrayOf(
                "2",
                "title",
                "1541768799000",
                "1541768800000",
                SYNC_ID_2,
                0,
                0,
                ETAG_SYNC_ID_2,
                SYNC_ID_2,
                "icshref2",
                "",
                0,
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )

        private val EVENT_NEW_FIELDS = arrayOf(
                "3",
                "title",
                "1541768799000",
                "1541768800000",
                SYNC_ID_NEW,
                0,
                0,
                ETAG_SYNC_ID_NEW,
                SYNC_ID_NEW,
                "icshref2",
                "",
                0,
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )

        private val EVENT_CHANGED = arrayOf(
                "4",
                "title",
                "1541768799000",
                "1541768800000",
                null,
                1,
                0,
                null,
                null,
                null,
                "",
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )

        private val EVENT_DELETED = arrayOf(
                "5",
                "title",
                "1541768799000",
                "1541768800000",
                null,
                0,
                1,
                null,
                null,
                null,
                "",
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )

        private val EVENT_CHILD = arrayOf(
                "6",
                "title child",
                "1541768799000",
                "1541768800000",
                null,
                0,
                1,
                null,
                null,
                null,
                "",
                0,
                null,
                1541768799000,
                null,
                "4",
                null,
                null,
                null,
                null,
                null
        )

        fun createMockCalendarCursor(): Cursor =
                MatrixCursor(CALENDAR_PROJECTION).apply {
                    val color = Color.parseColor("#aabbccee".substring(0, 7)).toString()
                    addRow(
                            arrayOf(
                                    "1",
                                    "account_name",
                                    "account_type",
                                    "name",
                                    "display_name",
                                    SYNC_ID_1,
                                    "ctag",
                                    color,
                                    "owner_account",
                                    "syncToken"
                            )
                    )

                    addRow(
                            arrayOf(
                                    "2",
                                    "account_name",
                                    "account_type",
                                    "name",
                                    "display_name",
                                    SYNC_ID_2,
                                    "ctag",
                                    Color.parseColor("#ffffff".substring(0, 7)),
                                    "owner_account",
                                    "syncToken"
                            )
                    )
                }

        fun createMockEvent1Cursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(EVENT1_FIELDS)
                }

        fun createMockEvent2Cursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(EVENT2_FIELDS)
                }

        fun createMockEventsAddCursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(EVENT1_FIELDS)
                    addRow(EVENT_NEW_FIELDS)
                }

        fun createMockEventsAllCursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(EVENT_NEW_FIELDS)
                    addRow(EVENT1_FIELDS)
                    addRow(EVENT2_FIELDS)
                }

        fun createMockEventsChangedCursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(EVENT_CHANGED)
                    addRow(EVENT_DELETED)
                }

        fun createMockEventsOriginalAndExceptedCursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(EVENT_CHANGED)
                    addRow(EVENT_CHILD)
                }

        fun createMockAlarmsCursor(): Cursor =
                MatrixCursor(AlarmEntityFactory.ALARM_PROJECTION).apply {
                    addRow(arrayOf(
                            10,
                            4,
                            CalendarContract.Reminders.METHOD_EMAIL.toString()

                    ))
                    addRow(arrayOf(
                            20,
                            6,
                            CalendarContract.Reminders.METHOD_EMAIL.toString()

                    ))
                }

        fun createMockAttendeeCursor(): Cursor =
                MatrixCursor(AttendeeEntityFactory.ATTENDEE_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "4",
                                    "1",
                                    "1",
                                    "1",
                                    "email@email.com",
                                    "Your Name"
                            )
                    )
                    addRow(
                            arrayOf(
                                    "6",
                                    "1",
                                    "1",
                                    "1",
                                    "email@email.com",
                                    "Your Child"
                            )
                    )
                }

    }
}
