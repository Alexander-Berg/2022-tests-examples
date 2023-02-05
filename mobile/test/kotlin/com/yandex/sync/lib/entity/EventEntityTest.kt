package com.yandex.sync.lib.entity

import biweekly.Biweekly
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.TimeZone

class EventEntityTest {

    @Test
    fun `Check correct exception millis and other`() {
        val iCalendar = Biweekly.parse(recurrenceWithAllDayException).first()
        val eventEntity = EventEntity.fromIcal(iCalendar, "icsHref", "etag")

        assertEquals(1, eventEntity.exceptionDates.size)
        assertEquals("t3v7mn28yandex.ru", eventEntity.uid)
        assertEquals(eventEntity.uid, eventEntity.syncId)
        assertEquals("OPAQUE", eventEntity.transparency)
        assertEquals("test", eventEntity.title)
        assertEquals("FREQ=WEEKLY;INTERVAL=1;BYDAY=TH", eventEntity.recurrenceRule)
        assertEquals(Date(1550737800000), eventEntity.startTime)
        assertEquals(Date(1550739600000), eventEntity.endTime)

        assertEquals(0, eventEntity.attendees.size)

        val exceptionDateEntity = eventEntity.exceptionDates.first()
        assertEquals(1551342600000, exceptionDateEntity.originalInstanceTime)
        assertEquals(false, exceptionDateEntity.isCanceled)
        assertEquals("etag", exceptionDateEntity.etag)
        assertEquals(true, exceptionDateEntity.allDay)
        assertEquals("icsHref", exceptionDateEntity.icsHref)
        assertEquals("OPAQUE", exceptionDateEntity.transparency)
        assertEquals("test", exceptionDateEntity.title)
        assertEquals(Date(1551214800000), exceptionDateEntity.startTime)
        assertEquals(Date(1551301200000), exceptionDateEntity.endTime)
        assertEquals(TimeZone.getTimeZone("Europe/Moscow"), exceptionDateEntity.timeZone)
    }

    @Test
    fun `Check attendee`() {
        val iCalendar = Biweekly.parse(withAttendee).first()
        val eventEntity = EventEntity.fromIcal(iCalendar, "icsHref", "etag")

        assertEquals(0, eventEntity.exceptionDates.size)
        assertEquals("vawLtmWeyandex.ru", eventEntity.uid)
        assertEquals("OPAQUE", eventEntity.transparency)
        assertEquals("событие с людьми", eventEntity.title)
        assertEquals(null, eventEntity.recurrenceRule)
        assertEquals(Date(1553338800000), eventEntity.startTime)
        assertEquals(Date(1553340600000), eventEntity.endTime)

        assertEquals(4, eventEntity.attendees.size)

        assertThat(eventEntity.attendees).containsExactlyInAnyOrder(
                AttendeeEntity(
                        name = "kvorobjaninov",
                        email = "kvorobjaninov@yandex.ru",
                        eventId = -1,
                        isOrganizer = true,
                        type = "UNKNOWN",
                        status = AttendeeEntity.Status.ACCEPTED
                ),
                AttendeeEntity(
                        name = "ttqul",
                        email = "ttqul@yandex.ru",
                        eventId = -1,
                        isOrganizer = false,
                        type = "UNKNOWN",
                        status = AttendeeEntity.Status.NONE
                ),
                AttendeeEntity(
                        name = "svyatoslavdp",
                        email = "svyatoslavdp@yandex.ru",
                        eventId = -1,
                        isOrganizer = false,
                        type = "UNKNOWN",
                        status = AttendeeEntity.Status.NONE
                ),
                AttendeeEntity(
                        name = "svyatoslavdp",
                        email = "svyatoslavdp@yandex-team.ru",
                        eventId = -1,
                        isOrganizer = false,
                        type = "UNKNOWN",
                        status = AttendeeEntity.Status.NONE
                )
        )
    }

    @Test
    fun `Check alarms`() {
        val iCalendar = Biweekly.parse(withAlarms).first()
        val eventEntity = EventEntity.fromIcal(iCalendar, "icsHref", "etag")

        assertEquals(0, eventEntity.exceptionDates.size)
        assertEquals("ibbhYeSJyandex.ru", eventEntity.uid)
        assertEquals("OPAQUE", eventEntity.transparency)
        assertEquals("alarms", eventEntity.title)
        assertEquals(null, eventEntity.recurrenceRule)
        assertEquals(Date(1553436000000), eventEntity.startTime)
        assertEquals(Date(1553437800000), eventEntity.endTime)

        assertEquals(1, eventEntity.alarms.size)

        assertThat(eventEntity.alarms).containsExactlyInAnyOrder(
                AlarmEntity(
                        minBeforeEvent = 900,
                        action = AlarmEntity.AlarmAction.DISPLAY
                )
        )
    }

    @Test
    fun `Check vevent generation`() {
        checkParseAndGeneration(withAlarms)
        checkParseAndGeneration(withAttendee)
        checkParseAndGeneration(recurrenceWithAllDayException)
    }

    private fun checkParseAndGeneration(icsOriginal: String) {
        val iCalendar = Biweekly.parse(icsOriginal).first()
        val eventEntity = EventEntity.fromIcal(iCalendar, "icsHref", "etag")

        val icalendarIcs = CalendarEntity(
                name = "Мои события",
                owner = "",
                color = "",
                displayName = "",
                ctag = "",
                href = "",
                syncToken = ""
        ).toIcalendar(eventEntity).write()

        val iCalendarParsed = Biweekly.parse(icalendarIcs).first()
        val eventEntityParsed = EventEntity.fromIcal(iCalendarParsed, "icsHref", "etag")

        assertEquals(eventEntity, eventEntityParsed)
    }

    @Test
    fun `generating Ical filte`() {
        val vEvent = EventEntity(
                id = 100,
                title = "title",
                startTime = Date(0),
                endTime = Date(100),
                icsHref = "href",
                recurrenceRule = "FREQ=WEEKLY;INTERVAL=1;BYDAY=WE",
                uid = "thhnxh8myandex.ru",
                transparency = EventEntity.OPAQUE,
                etag = "",
                location = "Location",
                description = "Description",
                exceptionDates = listOf(
                        EventEntity(
                                isCanceled = true,
                                originalInstanceTime = 1517258700000,
                                title = "",
                                startTime = Date(0),
                                endTime = Date(0),
                                icsHref = "",
                                uid = "",
                                etag = ""
                        )
                )
        ).toVevent()

        assertEquals("Location", vEvent.location.value)
        assertEquals("Description", vEvent.description.value)
        assertEquals("FREQ=WEEKLY;INTERVAL=1;BYDAY=WE", vEvent.recurrenceRule?.toICalString())
        assertEquals(1, vEvent.exceptionDates.size)
        assertEquals(1517258700000, vEvent.exceptionDates.first().values.first().time)


        val vEventEmptyFields = EventEntity(
                id = 100,
                title = "title",
                startTime = Date(0),
                endTime = Date(100),
                icsHref = "href",
                uid = "thhnxh8myandex.ru",
                etag = ""
        ).toVevent()

        assertEquals(null, vEventEmptyFields.location?.value)
        assertEquals(null, vEventEmptyFields.description?.value)
        assertEquals(null, vEventEmptyFields.recurrenceRule?.value)
        assertEquals(0, vEventEmptyFields.exceptionDates?.size)
    }

    companion object {
        const val withAlarms = "BEGIN:VCALENDAR\n" +
                "PRODID:-//Yandex LLC//Yandex Calendar//EN\n" +
                "VERSION:2.0\n" +
                "CALSCALE:GREGORIAN\n" +
                "METHOD:PUBLISH\n" +
                "BEGIN:VTIMEZONE\n" +
                "TZID:Europe/Moscow\n" +
                "TZURL:http://tzurl.org/zoneinfo/Europe/Moscow\n" +
                "X-LIC-LOCATION:Europe/Moscow\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+023017\n" +
                "TZOFFSETTO:+023017\n" +
                "TZNAME:MMT\n" +
                "DTSTART:18800101T000000\n" +
                "RDATE:18800101T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+023017\n" +
                "TZOFFSETTO:+023119\n" +
                "TZNAME:MMT\n" +
                "DTSTART:19160703T000000\n" +
                "RDATE:19160703T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+023119\n" +
                "TZOFFSETTO:+033119\n" +
                "TZNAME:MST\n" +
                "DTSTART:19170701T230000\n" +
                "RDATE:19170701T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+033119\n" +
                "TZOFFSETTO:+023119\n" +
                "TZNAME:MMT\n" +
                "DTSTART:19171228T000000\n" +
                "RDATE:19171228T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+023119\n" +
                "TZOFFSETTO:+043119\n" +
                "TZNAME:MDST\n" +
                "DTSTART:19180531T220000\n" +
                "RDATE:19180531T220000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+043119\n" +
                "TZOFFSETTO:+033119\n" +
                "TZNAME:MST\n" +
                "DTSTART:19180916T010000\n" +
                "RDATE:19180916T010000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+033119\n" +
                "TZOFFSETTO:+043119\n" +
                "TZNAME:MDST\n" +
                "DTSTART:19190531T230000\n" +
                "RDATE:19190531T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+043119\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19190701T043119\n" +
                "RDATE:19190701T043119\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:MSK\n" +
                "DTSTART:19190816T000000\n" +
                "RDATE:19190816T000000\n" +
                "RDATE:19211001T000000\n" +
                "RDATE:19811001T000000\n" +
                "RDATE:19821001T000000\n" +
                "RDATE:19831001T000000\n" +
                "RDATE:19840930T030000\n" +
                "RDATE:19850929T030000\n" +
                "RDATE:19860928T030000\n" +
                "RDATE:19870927T030000\n" +
                "RDATE:19880925T030000\n" +
                "RDATE:19890924T030000\n" +
                "RDATE:19900930T030000\n" +
                "RDATE:19920927T030000\n" +
                "RDATE:19930926T030000\n" +
                "RDATE:19940925T030000\n" +
                "RDATE:19950924T030000\n" +
                "RDATE:19961027T030000\n" +
                "RDATE:19971026T030000\n" +
                "RDATE:19981025T030000\n" +
                "RDATE:19991031T030000\n" +
                "RDATE:20001029T030000\n" +
                "RDATE:20011028T030000\n" +
                "RDATE:20021027T030000\n" +
                "RDATE:20031026T030000\n" +
                "RDATE:20041031T030000\n" +
                "RDATE:20051030T030000\n" +
                "RDATE:20061029T030000\n" +
                "RDATE:20071028T030000\n" +
                "RDATE:20081026T030000\n" +
                "RDATE:20091025T030000\n" +
                "RDATE:20101031T030000\n" +
                "RDATE:20141026T020000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19210214T230000\n" +
                "RDATE:19210214T230000\n" +
                "RDATE:19810401T000000\n" +
                "RDATE:19820401T000000\n" +
                "RDATE:19830401T000000\n" +
                "RDATE:19840401T000000\n" +
                "RDATE:19850331T020000\n" +
                "RDATE:19860330T020000\n" +
                "RDATE:19870329T020000\n" +
                "RDATE:19880327T020000\n" +
                "RDATE:19890326T020000\n" +
                "RDATE:19900325T020000\n" +
                "RDATE:19920329T020000\n" +
                "RDATE:19930328T020000\n" +
                "RDATE:19940327T020000\n" +
                "RDATE:19950326T020000\n" +
                "RDATE:19960331T020000\n" +
                "RDATE:19970330T020000\n" +
                "RDATE:19980329T020000\n" +
                "RDATE:19990328T020000\n" +
                "RDATE:20000326T020000\n" +
                "RDATE:20010325T020000\n" +
                "RDATE:20020331T020000\n" +
                "RDATE:20030330T020000\n" +
                "RDATE:20040328T020000\n" +
                "RDATE:20050327T020000\n" +
                "RDATE:20060326T020000\n" +
                "RDATE:20070325T020000\n" +
                "RDATE:20080330T020000\n" +
                "RDATE:20090329T020000\n" +
                "RDATE:20100328T020000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0500\n" +
                "TZNAME:+05\n" +
                "DTSTART:19210320T230000\n" +
                "RDATE:19210320T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0500\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19210901T000000\n" +
                "RDATE:19210901T000000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0200\n" +
                "TZNAME:EET\n" +
                "DTSTART:19221001T000000\n" +
                "RDATE:19221001T000000\n" +
                "RDATE:19910929T030000\n" +
                "END:STANDARD\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0200\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:MSK\n" +
                "DTSTART:19300621T000000\n" +
                "RDATE:19300621T000000\n" +
                "RDATE:19920119T020000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:EEST\n" +
                "DTSTART:19910331T020000\n" +
                "RDATE:19910331T020000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSK\n" +
                "DTSTART:20110327T020000\n" +
                "RDATE:20110327T020000\n" +
                "END:STANDARD\n" +
                "END:VTIMEZONE\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Europe/Moscow:20190324T170000\n" +
                "DTEND;TZID=Europe/Moscow:20190324T173000\n" +
                "SUMMARY:alarms\n" +
                "UID:ibbhYeSJyandex.ru\n" +
                "SEQUENCE:0\n" +
                "DTSTAMP:20190311T134438Z\n" +
                "CREATED:20190311T134429Z\n" +
                "URL:https://calendar.yandex.ru/event?event_id=885347097\n" +
                "TRANSP:OPAQUE\n" +
                "CATEGORIES:Мои события\n" +
                "LAST-MODIFIED:20190311T134429Z\n" +
                "BEGIN:VALARM\n" +
                "TRIGGER:-PT15H\n" +
                "ACTION:DISPLAY\n" +
                "END:VALARM\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR"

        const val withAttendee = "BEGIN:VCALENDAR\n" +
                "PRODID:-//Yandex LLC//Yandex Calendar//EN\n" +
                "VERSION:2.0\n" +
                "CALSCALE:GREGORIAN\n" +
                "METHOD:PUBLISH\n" +
                "BEGIN:VTIMEZONE\n" +
                "TZID:Europe/Moscow\n" +
                "TZURL:http://tzurl.org/zoneinfo/Europe/Moscow\n" +
                "X-LIC-LOCATION:Europe/Moscow\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+023017\n" +
                "TZOFFSETTO:+023017\n" +
                "TZNAME:MMT\n" +
                "DTSTART:18800101T000000\n" +
                "RDATE:18800101T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+023017\n" +
                "TZOFFSETTO:+023119\n" +
                "TZNAME:MMT\n" +
                "DTSTART:19160703T000000\n" +
                "RDATE:19160703T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+023119\n" +
                "TZOFFSETTO:+033119\n" +
                "TZNAME:MST\n" +
                "DTSTART:19170701T230000\n" +
                "RDATE:19170701T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+033119\n" +
                "TZOFFSETTO:+023119\n" +
                "TZNAME:MMT\n" +
                "DTSTART:19171228T000000\n" +
                "RDATE:19171228T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+023119\n" +
                "TZOFFSETTO:+043119\n" +
                "TZNAME:MDST\n" +
                "DTSTART:19180531T220000\n" +
                "RDATE:19180531T220000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+043119\n" +
                "TZOFFSETTO:+033119\n" +
                "TZNAME:MST\n" +
                "DTSTART:19180916T010000\n" +
                "RDATE:19180916T010000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+033119\n" +
                "TZOFFSETTO:+043119\n" +
                "TZNAME:MDST\n" +
                "DTSTART:19190531T230000\n" +
                "RDATE:19190531T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+043119\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19190701T043119\n" +
                "RDATE:19190701T043119\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:MSK\n" +
                "DTSTART:19190816T000000\n" +
                "RDATE:19190816T000000\n" +
                "RDATE:19211001T000000\n" +
                "RDATE:19811001T000000\n" +
                "RDATE:19821001T000000\n" +
                "RDATE:19831001T000000\n" +
                "RDATE:19840930T030000\n" +
                "RDATE:19850929T030000\n" +
                "RDATE:19860928T030000\n" +
                "RDATE:19870927T030000\n" +
                "RDATE:19880925T030000\n" +
                "RDATE:19890924T030000\n" +
                "RDATE:19900930T030000\n" +
                "RDATE:19920927T030000\n" +
                "RDATE:19930926T030000\n" +
                "RDATE:19940925T030000\n" +
                "RDATE:19950924T030000\n" +
                "RDATE:19961027T030000\n" +
                "RDATE:19971026T030000\n" +
                "RDATE:19981025T030000\n" +
                "RDATE:19991031T030000\n" +
                "RDATE:20001029T030000\n" +
                "RDATE:20011028T030000\n" +
                "RDATE:20021027T030000\n" +
                "RDATE:20031026T030000\n" +
                "RDATE:20041031T030000\n" +
                "RDATE:20051030T030000\n" +
                "RDATE:20061029T030000\n" +
                "RDATE:20071028T030000\n" +
                "RDATE:20081026T030000\n" +
                "RDATE:20091025T030000\n" +
                "RDATE:20101031T030000\n" +
                "RDATE:20141026T020000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19210214T230000\n" +
                "RDATE:19210214T230000\n" +
                "RDATE:19810401T000000\n" +
                "RDATE:19820401T000000\n" +
                "RDATE:19830401T000000\n" +
                "RDATE:19840401T000000\n" +
                "RDATE:19850331T020000\n" +
                "RDATE:19860330T020000\n" +
                "RDATE:19870329T020000\n" +
                "RDATE:19880327T020000\n" +
                "RDATE:19890326T020000\n" +
                "RDATE:19900325T020000\n" +
                "RDATE:19920329T020000\n" +
                "RDATE:19930328T020000\n" +
                "RDATE:19940327T020000\n" +
                "RDATE:19950326T020000\n" +
                "RDATE:19960331T020000\n" +
                "RDATE:19970330T020000\n" +
                "RDATE:19980329T020000\n" +
                "RDATE:19990328T020000\n" +
                "RDATE:20000326T020000\n" +
                "RDATE:20010325T020000\n" +
                "RDATE:20020331T020000\n" +
                "RDATE:20030330T020000\n" +
                "RDATE:20040328T020000\n" +
                "RDATE:20050327T020000\n" +
                "RDATE:20060326T020000\n" +
                "RDATE:20070325T020000\n" +
                "RDATE:20080330T020000\n" +
                "RDATE:20090329T020000\n" +
                "RDATE:20100328T020000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0500\n" +
                "TZNAME:+05\n" +
                "DTSTART:19210320T230000\n" +
                "RDATE:19210320T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0500\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19210901T000000\n" +
                "RDATE:19210901T000000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0200\n" +
                "TZNAME:EET\n" +
                "DTSTART:19221001T000000\n" +
                "RDATE:19221001T000000\n" +
                "RDATE:19910929T030000\n" +
                "END:STANDARD\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0200\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:MSK\n" +
                "DTSTART:19300621T000000\n" +
                "RDATE:19300621T000000\n" +
                "RDATE:19920119T020000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:EEST\n" +
                "DTSTART:19910331T020000\n" +
                "RDATE:19910331T020000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSK\n" +
                "DTSTART:20110327T020000\n" +
                "RDATE:20110327T020000\n" +
                "END:STANDARD\n" +
                "END:VTIMEZONE\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Europe/Moscow:20190323T140000\n" +
                "DTEND;TZID=Europe/Moscow:20190323T143000\n" +
                "SUMMARY:событие с людьми\n" +
                "UID:vawLtmWeyandex.ru\n" +
                "SEQUENCE:0\n" +
                "DTSTAMP:20190311T124406Z\n" +
                "CREATED:20190311T124359Z\n" +
                "URL:https://calendar.yandex.ru/event?event_id=885290492\n" +
                "TRANSP:OPAQUE\n" +
                "CATEGORIES:Мои события\n" +
                "ORGANIZER;CN=kvorobjaninov:mailto:kvorobjaninov@yandex.ru\n" +
                "ATTENDEE;PARTSTAT=ACCEPTED;CN=kvorobjaninov:mailto:kvorobjaninov@yandex.ru\n" +
                "ATTENDEE;PARTSTAT=NEEDS-ACTION;CN=ttqul:mailto:ttqul@yandex.ru\n" +
                "ATTENDEE;PARTSTAT=NEEDS-ACTION;CN=svyatoslavdp:mailto:svyatoslavdp@yandex.ru\n" +
                "ATTENDEE;PARTSTAT=NEEDS-ACTION;CN=svyatoslavdp:mailto:svyatoslavdp@yandex-team.ru\n" +
                "LAST-MODIFIED:20190311T124359Z\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR"

        const val recurrenceWithAllDayException = "BEGIN:VCALENDAR\n" +
                "PRODID:-//Yandex LLC//Yandex Calendar//EN\n" +
                "VERSION:2.0\n" +
                "CALSCALE:GREGORIAN\n" +
                "METHOD:PUBLISH\n" +
                "BEGIN:VTIMEZONE\n" +
                "TZID:Europe/Moscow\n" +
                "TZURL:http://tzurl.org/zoneinfo/Europe/Moscow\n" +
                "X-LIC-LOCATION:Europe/Moscow\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+023017\n" +
                "TZOFFSETTO:+023017\n" +
                "TZNAME:MMT\n" +
                "DTSTART:18800101T000000\n" +
                "RDATE:18800101T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+023017\n" +
                "TZOFFSETTO:+023119\n" +
                "TZNAME:MMT\n" +
                "DTSTART:19160703T000000\n" +
                "RDATE:19160703T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+023119\n" +
                "TZOFFSETTO:+033119\n" +
                "TZNAME:MST\n" +
                "DTSTART:19170701T230000\n" +
                "RDATE:19170701T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+033119\n" +
                "TZOFFSETTO:+023119\n" +
                "TZNAME:MMT\n" +
                "DTSTART:19171228T000000\n" +
                "RDATE:19171228T000000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+023119\n" +
                "TZOFFSETTO:+043119\n" +
                "TZNAME:MDST\n" +
                "DTSTART:19180531T220000\n" +
                "RDATE:19180531T220000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+043119\n" +
                "TZOFFSETTO:+033119\n" +
                "TZNAME:MST\n" +
                "DTSTART:19180916T010000\n" +
                "RDATE:19180916T010000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+033119\n" +
                "TZOFFSETTO:+043119\n" +
                "TZNAME:MDST\n" +
                "DTSTART:19190531T230000\n" +
                "RDATE:19190531T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+043119\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19190701T043119\n" +
                "RDATE:19190701T043119\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:MSK\n" +
                "DTSTART:19190816T000000\n" +
                "RDATE:19190816T000000\n" +
                "RDATE:19211001T000000\n" +
                "RDATE:19811001T000000\n" +
                "RDATE:19821001T000000\n" +
                "RDATE:19831001T000000\n" +
                "RDATE:19840930T030000\n" +
                "RDATE:19850929T030000\n" +
                "RDATE:19860928T030000\n" +
                "RDATE:19870927T030000\n" +
                "RDATE:19880925T030000\n" +
                "RDATE:19890924T030000\n" +
                "RDATE:19900930T030000\n" +
                "RDATE:19920927T030000\n" +
                "RDATE:19930926T030000\n" +
                "RDATE:19940925T030000\n" +
                "RDATE:19950924T030000\n" +
                "RDATE:19961027T030000\n" +
                "RDATE:19971026T030000\n" +
                "RDATE:19981025T030000\n" +
                "RDATE:19991031T030000\n" +
                "RDATE:20001029T030000\n" +
                "RDATE:20011028T030000\n" +
                "RDATE:20021027T030000\n" +
                "RDATE:20031026T030000\n" +
                "RDATE:20041031T030000\n" +
                "RDATE:20051030T030000\n" +
                "RDATE:20061029T030000\n" +
                "RDATE:20071028T030000\n" +
                "RDATE:20081026T030000\n" +
                "RDATE:20091025T030000\n" +
                "RDATE:20101031T030000\n" +
                "RDATE:20141026T020000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19210214T230000\n" +
                "RDATE:19210214T230000\n" +
                "RDATE:19810401T000000\n" +
                "RDATE:19820401T000000\n" +
                "RDATE:19830401T000000\n" +
                "RDATE:19840401T000000\n" +
                "RDATE:19850331T020000\n" +
                "RDATE:19860330T020000\n" +
                "RDATE:19870329T020000\n" +
                "RDATE:19880327T020000\n" +
                "RDATE:19890326T020000\n" +
                "RDATE:19900325T020000\n" +
                "RDATE:19920329T020000\n" +
                "RDATE:19930328T020000\n" +
                "RDATE:19940327T020000\n" +
                "RDATE:19950326T020000\n" +
                "RDATE:19960331T020000\n" +
                "RDATE:19970330T020000\n" +
                "RDATE:19980329T020000\n" +
                "RDATE:19990328T020000\n" +
                "RDATE:20000326T020000\n" +
                "RDATE:20010325T020000\n" +
                "RDATE:20020331T020000\n" +
                "RDATE:20030330T020000\n" +
                "RDATE:20040328T020000\n" +
                "RDATE:20050327T020000\n" +
                "RDATE:20060326T020000\n" +
                "RDATE:20070325T020000\n" +
                "RDATE:20080330T020000\n" +
                "RDATE:20090329T020000\n" +
                "RDATE:20100328T020000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0500\n" +
                "TZNAME:+05\n" +
                "DTSTART:19210320T230000\n" +
                "RDATE:19210320T230000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0500\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSD\n" +
                "DTSTART:19210901T000000\n" +
                "RDATE:19210901T000000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0200\n" +
                "TZNAME:EET\n" +
                "DTSTART:19221001T000000\n" +
                "RDATE:19221001T000000\n" +
                "RDATE:19910929T030000\n" +
                "END:STANDARD\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0200\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:MSK\n" +
                "DTSTART:19300621T000000\n" +
                "RDATE:19300621T000000\n" +
                "RDATE:19920119T020000\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0300\n" +
                "TZNAME:EEST\n" +
                "DTSTART:19910331T020000\n" +
                "RDATE:19910331T020000\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "TZNAME:MSK\n" +
                "DTSTART:20110327T020000\n" +
                "RDATE:20110327T020000\n" +
                "END:STANDARD\n" +
                "END:VTIMEZONE\n" +
                "\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Europe/Moscow:20190221T113000\n" +
                "DTEND;TZID=Europe/Moscow:20190221T120000\n" +
                "SUMMARY:test\n" +
                "UID:t3v7mn28yandex.ru\n" +
                "SEQUENCE:1\n" +
                "DTSTAMP:20190220T102013Z\n" +
                "CREATED:20190220T100906Z\n" +
                "URL:https://calendar.yandex.ru/event?event_id=859429720\n" +
                "RRULE:FREQ=WEEKLY;BYDAY=TH;INTERVAL=1\n" +
                "TRANSP:OPAQUE\n" +
                "CATEGORIES:Мои события\n" +
                "LAST-MODIFIED:20190220T100906Z\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;VALUE=DATE:20190227\n" +
                "DTEND;VALUE=DATE:20190228\n" +
                "SUMMARY:test\n" +
                "UID:t3v7mn28yandex.ru\n" +
                "SEQUENCE:3\n" +
                "DTSTAMP:20190220T102013Z\n" +
                "CREATED:20190220T100927Z\n" +
                "URL:https://calendar.yandex.ru/event?event_id=859430027\n" +
                "RECURRENCE-ID;VALUE=DATE:20190228\n" +
                "TRANSP:OPAQUE\n" +
                "CATEGORIES:Мои события\n" +
                "LAST-MODIFIED:20190220T101931Z\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\n"
    }
}