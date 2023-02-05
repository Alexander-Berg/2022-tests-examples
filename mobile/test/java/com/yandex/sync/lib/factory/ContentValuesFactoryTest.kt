package com.yandex.sync.lib.factory

import android.accounts.Account
import android.graphics.Color
import android.provider.CalendarContract
import com.yandex.sync.lib.entity.AlarmEntity
import com.yandex.sync.lib.entity.AttendeeEntity
import com.yandex.sync.lib.entity.CalendarEntity
import com.yandex.sync.lib.entity.EventEntity
import com.yandex.sync.lib.factory.CalendarEntityFactory.CALENDAR_CTAG
import com.yandex.sync.lib.factory.CalendarEntityFactory.CALENDAR_SYNC_TOKEN
import com.yandex.sync.lib.factory.EventEntityFactory.EVENT_COLUMN_ETAG
import com.yandex.sync.lib.factory.EventEntityFactory.EVENT_COLUMN_ICS_HREF
import com.yandex.sync.lib.factory.EventEntityFactory.EVENT_COLUMN_UID
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(SyncTestRunner::class)
class ContentValuesFactoryTest {

    @Test
    fun `alarmEntity display action test`() {
        val alarmEntity = AlarmEntity(
                minBeforeEvent = 10,
                action = AlarmEntity.AlarmAction.DISPLAY
        )
        val contentValues = ContentValuesFactory.fromAlarmEntity(alarmEntity, 1)

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Reminders.EVENT_ID))
        assertEquals(10L, contentValues.getAsLong(CalendarContract.Reminders.MINUTES))
        assertEquals(CalendarContract.Reminders.METHOD_ALERT,
                contentValues.getAsInteger(CalendarContract.Reminders.METHOD))
    }

    @Test
    fun `alarmEntity audio action test`() {
        val alarmEntity = AlarmEntity(
                minBeforeEvent = 10,
                action = AlarmEntity.AlarmAction.AUDIO
        )
        val contentValues = ContentValuesFactory.fromAlarmEntity(alarmEntity, 1)

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Reminders.EVENT_ID))
        assertEquals(10L, contentValues.getAsLong(CalendarContract.Reminders.MINUTES))
        assertEquals(CalendarContract.Reminders.METHOD_DEFAULT,
                contentValues.getAsInteger(CalendarContract.Reminders.METHOD))
    }

    @Test
    fun `alarmEntity email action test`() {
        val alarmEntity = AlarmEntity(
                minBeforeEvent = 10,
                action = AlarmEntity.AlarmAction.EMAIL
        )
        val contentValues = ContentValuesFactory.fromAlarmEntity(alarmEntity, 1)

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Reminders.EVENT_ID))
        assertEquals(10L, contentValues.getAsLong(CalendarContract.Reminders.MINUTES))
        assertEquals(CalendarContract.Reminders.METHOD_EMAIL,
                contentValues.getAsInteger(CalendarContract.Reminders.METHOD))
    }

    @Test
    fun `alarmEntity procedure action test`() {
        val alarmEntity = AlarmEntity(
                minBeforeEvent = 10,
                action = AlarmEntity.AlarmAction.PROCEDURE
        )
        val contentValues = ContentValuesFactory.fromAlarmEntity(alarmEntity, 1)

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Reminders.EVENT_ID))
        assertEquals(10L, contentValues.getAsLong(CalendarContract.Reminders.MINUTES))
        assertEquals(CalendarContract.Reminders.METHOD_DEFAULT,
                contentValues.getAsInteger(CalendarContract.Reminders.METHOD))
    }

    @Test
    fun `attendeeEntity accepted status test`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.ACCEPTED,
                type = "UNKNOWN",
                isOrganizer = false
        )

        val contentValues = ContentValuesFactory.fromAttendeeEntity(entity, 1)

        assertEquals("Svyatoslav",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_NAME))

        assertEquals("svyatoslavdp@yandex-team.ru",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))

        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS))

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Attendees.EVENT_ID))

        assertEquals(CalendarContract.Attendees.RELATIONSHIP_ATTENDEE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP))

        assertEquals(CalendarContract.Attendees.TYPE_NONE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE))
    }

    @Test
    fun `attendeeEntity declined status test`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.DECLINED,
                type = "ROOM",
                isOrganizer = true
        )

        val contentValues = ContentValuesFactory.fromAttendeeEntity(entity, 1)

        assertEquals("Svyatoslav",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_NAME))

        assertEquals("svyatoslavdp@yandex-team.ru",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))

        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS))

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Attendees.EVENT_ID))

        assertEquals(CalendarContract.Attendees.RELATIONSHIP_ORGANIZER,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP))

        assertEquals(CalendarContract.Attendees.TYPE_RESOURCE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE))
    }

    @Test
    fun `attendeeEntity tentative status test`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.TENTATIVE,
                type = "GROUP",
                isOrganizer = true
        )

        val contentValues = ContentValuesFactory.fromAttendeeEntity(entity, 1)

        assertEquals("Svyatoslav",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_NAME))

        assertEquals("svyatoslavdp@yandex-team.ru",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))

        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS))

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Attendees.EVENT_ID))

        assertEquals(CalendarContract.Attendees.RELATIONSHIP_ORGANIZER,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP))

        assertEquals(CalendarContract.Attendees.TYPE_REQUIRED,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE))
    }

    @Test
    fun `attendeeEntity needs_action status test`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.NEEDS_ACTION,
                type = "GROUP",
                isOrganizer = false
        )

        val contentValues = ContentValuesFactory.fromAttendeeEntity(entity, 1)

        assertEquals("Svyatoslav",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_NAME))

        assertEquals("svyatoslavdp@yandex-team.ru",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))

        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_NONE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS))

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Attendees.EVENT_ID))

        assertEquals(CalendarContract.Attendees.RELATIONSHIP_ATTENDEE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP))

        assertEquals(CalendarContract.Attendees.TYPE_REQUIRED,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE))
    }

    @Test
    fun `attendeeEntity delegated status test`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.DELEGATED,
                type = "RESOURCE",
                isOrganizer = false
        )

        val contentValues = ContentValuesFactory.fromAttendeeEntity(entity, 1)

        assertEquals("Svyatoslav",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_NAME))

        assertEquals("svyatoslavdp@yandex-team.ru",
                contentValues.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))

        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_NONE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS))

        assertEquals(1L, contentValues.getAsLong(CalendarContract.Attendees.EVENT_ID))

        assertEquals(CalendarContract.Attendees.RELATIONSHIP_ATTENDEE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP))

        assertEquals(CalendarContract.Attendees.TYPE_RESOURCE,
                contentValues.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE))
    }

    @Test
    fun `attendeeEntity none status test`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.NONE,
                type = "",
                isOrganizer = false
        )

        val cv = ContentValuesFactory.fromAttendeeEntity(entity, 1)

        assertEquals("Svyatoslav", cv.getAsString(CalendarContract.Attendees.ATTENDEE_NAME))

        assertEquals("svyatoslavdp@yandex-team.ru",
                cv.getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))

        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_NONE,
                cv.getAsInteger(CalendarContract.Attendees.ATTENDEE_STATUS))

        assertEquals(1L, cv.getAsLong(CalendarContract.Attendees.EVENT_ID))

        assertEquals(CalendarContract.Attendees.RELATIONSHIP_ATTENDEE,
                cv.getAsInteger(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP))

        assertEquals(CalendarContract.Attendees.TYPE_NONE,
                cv.getAsInteger(CalendarContract.Attendees.ATTENDEE_TYPE))
    }

    @Test
    fun `CalendarEntity simple test`() {
        val entity = CalendarEntity(
                name = "name",
                displayName = "displayName",
                color = "#ffffff",
                owner = "ttqul@yandex.ru",
                href = "path/to/calendar",
                ctag = "13122312",
                syncToken = "aaadd"
        )

        val account = Account("ttqul@ya.ru", "yandex")
        val cv = ContentValuesFactory.fromCalendarEntity(entity, account)

        assertEquals("ttqul@ya.ru", cv.getAsString(CalendarContract.Calendars.ACCOUNT_NAME))

        assertEquals("yandex", cv.getAsString(CalendarContract.Calendars.ACCOUNT_TYPE))

        assertEquals("aaadd", cv.getAsString(CALENDAR_SYNC_TOKEN))

        assertEquals("13122312", cv.getAsString(CALENDAR_CTAG))

        assertEquals("path/to/calendar", cv.getAsString(CalendarContract.Calendars._SYNC_ID))

        assertEquals("name", cv.getAsString(CalendarContract.Calendars.NAME))

        assertEquals("displayName", cv.getAsString(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))

        assertEquals(true, cv.getAsBoolean(CalendarContract.Calendars.SYNC_EVENTS))

        assertEquals(true, cv.getAsBoolean(CalendarContract.Calendars.VISIBLE))

        assertEquals(Color.WHITE, cv.getAsInteger(CalendarContract.Calendars.CALENDAR_COLOR))

        val availability = arrayOf(
                CalendarContract.Events.AVAILABILITY_BUSY,
                CalendarContract.Events.AVAILABILITY_FREE,
                CalendarContract.Events.AVAILABILITY_TENTATIVE
        ).joinToString(",")

        val reminders = arrayOf(
                CalendarContract.Reminders.METHOD_EMAIL,
                CalendarContract.Reminders.METHOD_DEFAULT,
                CalendarContract.Reminders.METHOD_ALARM,
                CalendarContract.Reminders.METHOD_ALERT
        ).joinToString(",")

        assertEquals(availability, cv.getAsString(CalendarContract.Calendars.ALLOWED_AVAILABILITY))
        assertEquals(reminders, cv.getAsString(CalendarContract.Calendars.ALLOWED_REMINDERS))
    }

    @Test
    fun `CalendarEntity broken color test`() {
        val entity = CalendarEntity(
                name = "name",
                displayName = "displayName",
                color = "#5mmnnmnnnm",
                owner = "ttqul@yandex.ru",
                href = "path/to/calendar",
                ctag = "13122312",
                syncToken = "aaadd"
        )

        val account = Account("ttqul@ya.ru", "yandex")
        val cv = ContentValuesFactory.fromCalendarEntity(entity, account)

        assertEquals("ttqul@ya.ru", cv.getAsString(CalendarContract.Calendars.ACCOUNT_NAME))

        assertEquals("yandex", cv.getAsString(CalendarContract.Calendars.ACCOUNT_TYPE))

        assertEquals("aaadd", cv.getAsString(CALENDAR_SYNC_TOKEN))

        assertEquals("13122312", cv.getAsString(CALENDAR_CTAG))

        assertEquals("path/to/calendar", cv.getAsString(CalendarContract.Calendars._SYNC_ID))

        assertEquals("name", cv.getAsString(CalendarContract.Calendars.NAME))

        assertEquals("displayName", cv.getAsString(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))

        assertEquals(true, cv.getAsBoolean(CalendarContract.Calendars.SYNC_EVENTS))

        assertEquals(true, cv.getAsBoolean(CalendarContract.Calendars.VISIBLE))

        assertEquals(Color.BLACK, cv.getAsInteger(CalendarContract.Calendars.CALENDAR_COLOR))

        val availability = arrayOf(
                CalendarContract.Events.AVAILABILITY_BUSY,
                CalendarContract.Events.AVAILABILITY_FREE,
                CalendarContract.Events.AVAILABILITY_TENTATIVE
        ).joinToString(",")

        val reminders = arrayOf(
                CalendarContract.Reminders.METHOD_EMAIL,
                CalendarContract.Reminders.METHOD_DEFAULT,
                CalendarContract.Reminders.METHOD_ALARM,
                CalendarContract.Reminders.METHOD_ALERT
        ).joinToString(",")

        assertEquals(availability, cv.getAsString(CalendarContract.Calendars.ALLOWED_AVAILABILITY))
        assertEquals(reminders, cv.getAsString(CalendarContract.Calendars.ALLOWED_REMINDERS))
    }


    @Test
    fun `CalendarEntity with id without sync and visible params`() {
        val entity = CalendarEntity(
                calendarId = 100,
                name = "name",
                displayName = "displayName",
                color = "#5mmnnmnnnm",
                owner = "ttqul@yandex.ru",
                href = "path/to/calendar",
                ctag = "13122312",
                syncToken = "aaadd"
        )

        val account = Account("ttqul@ya.ru", "yandex")
        val cv = ContentValuesFactory.fromCalendarEntity(entity, account)

        assertEquals(null, cv.getAsBoolean(CalendarContract.Calendars.SYNC_EVENTS))

        assertEquals(null, cv.getAsBoolean(CalendarContract.Calendars.VISIBLE))
    }

    @Test
    fun `EventEntity with recurrence rule`() {
        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1542189000000),
                endTime = Date(1542190000000),
                icsHref = "href",
                recurrenceRule = "FREQ=WEEKLY;BYDAY=WE;INTERVAL=1",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                url = "urlevent",
                etag = "etag"
        )

        val cv = ContentValuesFactory.fromEventEntity(entity, 10)

        assertEquals("Test Event", cv.getAsString(CalendarContract.Events.TITLE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(CalendarContract.Events._SYNC_ID))

        assertEquals(10, cv.getAsLong(CalendarContract.Events.CALENDAR_ID))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(EVENT_COLUMN_UID))

        assertEquals("href", cv.getAsString(EVENT_COLUMN_ICS_HREF))

        assertEquals(CalendarContract.Events.AVAILABILITY_BUSY,
                cv.getAsInteger(CalendarContract.Events.AVAILABILITY))

        assertEquals(1542189000000, cv.getAsLong(CalendarContract.Events.DTSTART))

        val asLong: Long? = cv.getAsLong(CalendarContract.Events.DTEND)
        assertEquals(null, asLong)

        assertEquals("PT16M40S", cv.getAsString(CalendarContract.Events.DURATION))

        assertEquals("FREQ=WEEKLY;BYDAY=WE;INTERVAL=1", cv.getAsString(CalendarContract.Events.RRULE))

        assertEquals(null, cv.getAsString(CalendarContract.Events.EVENT_LOCATION))

        assertEquals(null, cv.getAsString(CalendarContract.Events.DESCRIPTION))

        assertEquals(null, cv.getAsString(CalendarContract.Events.EVENT_TIMEZONE))

        assertEquals(0, cv.getAsInteger(CalendarContract.Events.ALL_DAY))

        assertEquals("etag", cv.getAsString(EVENT_COLUMN_ETAG))

        assertEquals(null, cv.getAsString(CalendarContract.Events.ORIGINAL_SYNC_ID))
    }

    @Test
    fun `EventEntity without recurrence rule`() {
        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1542189000000),
                endTime = Date(1542190000000),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                url = "urlevent",
                location = "location",
                description = "description",
                allDay = false,
                timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                originalSyncId = "originalSyncId",
                etag = "etag"
        )

        val cv = ContentValuesFactory.fromEventEntity(entity, 10)

        assertEquals("Test Event", cv.getAsString(CalendarContract.Events.TITLE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(CalendarContract.Events._SYNC_ID))

        assertEquals(10, cv.getAsLong(CalendarContract.Events.CALENDAR_ID))

        assertEquals(1542189000000, cv.getAsLong(CalendarContract.Events.DTSTART))

        assertEquals(1542190000000, cv.getAsLong(CalendarContract.Events.DTEND))

        assertEquals(null, cv.getAsString(CalendarContract.Events.DURATION))

        assertEquals(null, cv.getAsString(CalendarContract.Events.RRULE))

        assertEquals("location", cv.getAsString(CalendarContract.Events.EVENT_LOCATION))

        assertEquals("description", cv.getAsString(CalendarContract.Events.DESCRIPTION))

        assertEquals("Europe/Moscow", cv.getAsString(CalendarContract.Events.EVENT_TIMEZONE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(EVENT_COLUMN_UID))

        assertEquals("href", cv.getAsString(EVENT_COLUMN_ICS_HREF))

        assertEquals("etag", cv.getAsString(EVENT_COLUMN_ETAG))

        assertEquals(CalendarContract.Events.AVAILABILITY_BUSY,
                cv.getAsInteger(CalendarContract.Events.AVAILABILITY))

        assertEquals(0, cv.getAsInteger(CalendarContract.Events.ALL_DAY))
    }

    @Test
    fun `EventEntity allDay event in UTC+`() {
        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1550696400000),
                endTime = Date(1550869200000),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.TRANSPARENT,
                url = "urlevent",
                location = "location",
                description = "description",
                allDay = true,
                timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                etag = "etag"
        )

        val cv = ContentValuesFactory.fromEventEntity(entity, 10)

        assertEquals("Test Event", cv.getAsString(CalendarContract.Events.TITLE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(CalendarContract.Events._SYNC_ID))

        assertEquals(10, cv.getAsLong(CalendarContract.Events.CALENDAR_ID))

        assertEquals(1550707200000, cv.getAsLong(CalendarContract.Events.DTSTART))

        assertEquals(1550880000000, cv.getAsLong(CalendarContract.Events.DTEND))

        assertEquals(null, cv.getAsString(CalendarContract.Events.DURATION))

        assertEquals(null, cv.getAsString(CalendarContract.Events.RRULE))

        assertEquals("location", cv.getAsString(CalendarContract.Events.EVENT_LOCATION))

        assertEquals("description", cv.getAsString(CalendarContract.Events.DESCRIPTION))

        assertEquals("UTC", cv.getAsString(CalendarContract.Events.EVENT_TIMEZONE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(EVENT_COLUMN_UID))

        assertEquals("href", cv.getAsString(EVENT_COLUMN_ICS_HREF))

        assertEquals("etag", cv.getAsString(EVENT_COLUMN_ETAG))

        assertEquals(CalendarContract.Events.AVAILABILITY_FREE,
                cv.getAsInteger(CalendarContract.Events.AVAILABILITY))

        assertEquals(1, cv.getAsInteger(CalendarContract.Events.ALL_DAY))

        assertEquals(null, cv.getAsString(CalendarContract.Events.ORIGINAL_SYNC_ID))
    }

    @Test
    fun `EventEntity allDay event in UTC-`() {
        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1550908800000),
                endTime = Date(1551081600000),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.TRANSPARENT,
                url = "urlevent",
                location = "location",
                description = "description",
                allDay = true,
                timeZone = TimeZone.getTimeZone("America/Los_Angeles"),
                etag = "etag"
        )

        val cv = ContentValuesFactory.fromEventEntity(entity, 10)

        assertEquals("Test Event", cv.getAsString(CalendarContract.Events.TITLE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(CalendarContract.Events._SYNC_ID))

        assertEquals(10, cv.getAsLong(CalendarContract.Events.CALENDAR_ID))

        assertEquals(1550880000000, cv.getAsLong(CalendarContract.Events.DTSTART))

        assertEquals(1551052800000, cv.getAsLong(CalendarContract.Events.DTEND))

        assertEquals(null, cv.getAsString(CalendarContract.Events.DURATION))

        assertEquals(null, cv.getAsString(CalendarContract.Events.RRULE))

        assertEquals("location", cv.getAsString(CalendarContract.Events.EVENT_LOCATION))

        assertEquals("description", cv.getAsString(CalendarContract.Events.DESCRIPTION))

        assertEquals("UTC", cv.getAsString(CalendarContract.Events.EVENT_TIMEZONE))

        assertEquals("thhnxh8myandex.ru", cv.getAsString(EVENT_COLUMN_UID))

        assertEquals(null, cv.getAsString(CalendarContract.Events.ORIGINAL_SYNC_ID))

        assertEquals("href", cv.getAsString(EVENT_COLUMN_ICS_HREF))

        assertEquals("etag", cv.getAsString(EVENT_COLUMN_ETAG))

        assertEquals(CalendarContract.Events.AVAILABILITY_FREE,
                cv.getAsInteger(CalendarContract.Events.AVAILABILITY))

        assertEquals(1, cv.getAsInteger(CalendarContract.Events.ALL_DAY))
    }

    @Test
    fun `uploaded EventEntity has dirty 0`() {

        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1550908800000),
                endTime = Date(1551081600000),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.TRANSPARENT,
                url = "urlevent",
                location = "location",
                description = "description",
                allDay = true,
                timeZone = TimeZone.getTimeZone("America/Los_Angeles"),
                etag = "etag"
        )

        val cv = ContentValuesFactory.fromUploadedEventEntity(entity)
        assertEquals(0, cv.getAsInteger(CalendarContract.Events.DIRTY))
        assertEquals("thhnxh8myandex.ru", cv.getAsString(CalendarContract.Events._SYNC_ID))
        assertEquals("etag", cv.getAsString(EVENT_COLUMN_ETAG))
        assertEquals("href", cv.getAsString(EVENT_COLUMN_ICS_HREF))
    }

    @Test
    fun `exception date content values all day`() {
        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1550908800000),
                endTime = Date(1551081600000),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.TRANSPARENT,
                url = "urlevent",
                location = "location",
                description = "description",
                allDay = true,
                timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                etag = "etag",
                originalInstanceTime = 1550908000000
        )

        val cv = ContentValuesFactory.fromExceptionDate(entity)

        assertEquals(1550908000000, cv.getAsLong(CalendarContract.Events.ORIGINAL_INSTANCE_TIME))
        assertEquals(1550919600000, cv.getAsLong(CalendarContract.Events.DTSTART))
        assertEquals(null, cv.getAsLong(CalendarContract.Events.DTEND))
        assertEquals(1, cv.getAsInteger(CalendarContract.Events.ALL_DAY))
        assertEquals(true, cv.getAsBoolean(CalendarContract.Events.HAS_ATTENDEE_DATA))
        assertEquals("UTC", cv.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
        assertEquals("P2D", cv.getAsString(CalendarContract.Events.DURATION))
        assertEquals("Test Event", cv.getAsString(CalendarContract.Events.TITLE))
        assertEquals("description", cv.getAsString(CalendarContract.Events.DESCRIPTION))
        assertEquals("location", cv.getAsString(CalendarContract.Events.EVENT_LOCATION))
        assertEquals(CalendarContract.Events.STATUS_CONFIRMED,
                cv.getAsInteger(CalendarContract.Events.STATUS))

        assertEquals(CalendarContract.Events.AVAILABILITY_FREE,
                cv.getAsInteger(CalendarContract.Events.AVAILABILITY))

    }

    @Test
    fun `exception date content values`() {
        val entity = EventEntity(
                title = "Test Event",
                startTime = Date(1550908800000),
                endTime = Date(1550909000000),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                url = "urlevent",
                location = "location",
                description = "description",
                allDay = false,
                timeZone = TimeZone.getTimeZone("Europe/Moscow"),
                etag = "etag",
                originalInstanceTime = 1550908000000
        )

        val cv = ContentValuesFactory.fromExceptionDate(entity)

        assertEquals(1550908000000, cv.getAsLong(CalendarContract.Events.ORIGINAL_INSTANCE_TIME))
        assertEquals(1550908800000, cv.getAsLong(CalendarContract.Events.DTSTART))
        assertEquals(null, cv.getAsLong(CalendarContract.Events.DTEND))
        assertEquals(0, cv.getAsInteger(CalendarContract.Events.ALL_DAY))
        assertEquals(true, cv.getAsBoolean(CalendarContract.Events.HAS_ATTENDEE_DATA))
        assertEquals("Europe/Moscow", cv.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
        assertEquals("PT3M20S", cv.getAsString(CalendarContract.Events.DURATION))
        assertEquals("Test Event", cv.getAsString(CalendarContract.Events.TITLE))
        assertEquals("description", cv.getAsString(CalendarContract.Events.DESCRIPTION))
        assertEquals("location", cv.getAsString(CalendarContract.Events.EVENT_LOCATION))
        assertEquals(CalendarContract.Events.STATUS_CONFIRMED,
                cv.getAsInteger(CalendarContract.Events.STATUS))

        assertEquals(CalendarContract.Events.AVAILABILITY_BUSY,
                cv.getAsInteger(CalendarContract.Events.AVAILABILITY))

    }
}
