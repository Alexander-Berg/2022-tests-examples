package com.yandex.sync.lib.factory

import android.accounts.Account
import android.content.SyncResult
import android.net.Uri
import com.yandex.sync.lib.entity.AlarmEntity
import com.yandex.sync.lib.entity.AttendeeEntity
import com.yandex.sync.lib.entity.CalendarEntity
import com.yandex.sync.lib.entity.EventEntity
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(SyncTestRunner::class)
class ContentProviderOperationFactoryTest {

    private lateinit var operationFactory: ContentProviderOperationFactory
    lateinit var syncResult: SyncResult

    @Before
    fun setUp() {
        syncResult = SyncResult()
        operationFactory = ContentProviderOperationFactory(Account("name", "type"), syncResult)
    }

    @Test
    fun `insertExceptionDate simple`() {
        val operation = operationFactory.insertExceptionDate(EventEntity(
                title = "title",
                startTime = Date(0),
                endTime = Date(100),
                icsHref = "",
                uid = "thhnxh8myandex.ru",
                etag = ""
        ), 10)
        val expected = "content://com.android.calendar/exception/10?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isInsert)
    }

    @Test
    fun `deleteExceptionDate simple`() {
        val operation = operationFactory.deleteExceptionDate(100, 10)
        val expected = "content://com.android.calendar/exception/100/10?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isDelete)
    }

    @Test
    fun `deleteAttendee simple`() {
        val operation = operationFactory.deleteAttendee(10)
        val expected = "content://com.android.calendar/attendees?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isDelete)
    }

    @Test
    fun `insertAttendee simple`() {
        val operation = operationFactory.insertAttendee(AttendeeEntity(
                "name",
                "email",
                AttendeeEntity.Status.ACCEPTED,
                type = "UNKNOWN",
                isOrganizer = false
        ), 10)
        val expected = "content://com.android.calendar/attendees?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isInsert)
    }

    @Test
    fun `deleteReminder simple`() {
        val operation = operationFactory.deleteReminder(10)
        val expected = "content://com.android.calendar/reminders?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isDelete)
    }

    @Test
    fun `insertReminder simple`() {
        val operation = operationFactory.insertReminder(AlarmEntity(10, AlarmEntity.AlarmAction.EMAIL), 10)
        val expected = "content://com.android.calendar/reminders?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isInsert)
    }

    @Test
    fun `insertEvent simple`() {
        val operation = operationFactory.insertEventEntity(EventEntity(
                id = -1,
                title = "title",
                startTime = Date(0),
                endTime = Date(100),
                icsHref = "href",
                recurrenceRule = "FREQ=WEEKLY;BYDAY=WE;INTERVAL=1",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                etag = ""
        ), 10)
        val expected = "content://com.android.calendar/events?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isInsert)
    }

    @Test
    fun `updateEvent simple`() {
        val operation = operationFactory.updateEventEntity(EventEntity(
                id = 100,
                title = "title",
                startTime = Date(0),
                endTime = Date(100),
                icsHref = "href",
                recurrenceRule = "FREQ=WEEKLY;BYDAY=WE;INTERVAL=1",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                etag = ""
        ), 10)
        val expected = "content://com.android.calendar/events/100?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isUpdate)
    }

    @Test
    fun updateUploadedEventEntityTest() {
        val operation = operationFactory.updateUploadedEventEntity(EventEntity(
                id = 100,
                title = "title",
                startTime = Date(0),
                endTime = Date(100),
                icsHref = "href",
                recurrenceRule = "FREQ=WEEKLY;BYDAY=WE;INTERVAL=1",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                etag = ""
        ))
        val expected = "content://com.android.calendar/events/100?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isUpdate)
    }

    @Test
    fun `deleteEvent simple`() {
        val operation = operationFactory.deleteEventEntity(10)
        val expected = "content://com.android.calendar/events?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isDelete)
    }

    @Test
    fun `insertCalendar simple`() {
        val operation = operationFactory.insertCalendarEntity(CalendarEntity(
                calendarId = -1,
                name = "title",
                displayName = "displayName",
                color = "#ffffff",
                owner = "svyatoslavdp@gmail.com",
                href = "href",
                ctag = "ctag",
                syncToken = "syncToken"
        ))
        val expected = "content://com.android.calendar/calendars?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isInsert)
    }

    @Test
    fun `updateCalendar simple`() {
        val operation = operationFactory.updateCalendarEntity(CalendarEntity(
                calendarId = 10,
                name = "title",
                displayName = "displayName",
                color = "#ffffff",
                owner = "svyatoslavdp@gmail.com",
                href = "href",
                ctag = "ctag",
                syncToken = "syncToken"
        ))
        val expected = "content://com.android.calendar/calendars/10?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isUpdate)
    }

    @Test
    fun `deleteCalendar simple`() {
        val operation = operationFactory.deleteCalendarEntity(10)
        val expected = "content://com.android.calendar/calendars/10?caller_is_syncadapter=true&account_name=name&account_type=type"
        assertEquals(Uri.parse(expected), operation.uri)
        assertTrue(operation.isDelete)
    }

}
