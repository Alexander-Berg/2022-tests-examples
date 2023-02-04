from datetime import timedelta
from unittest.mock import MagicMock, patch

import pytz

from django.conf import settings
from django.test import TestCase, override_settings
from django.utils.dateparse import parse_datetime

from lms.calendars.services import (
    CalendarException, _update_timeslot_dates, delete_calendar_event, get_or_create_calendar_event,
    get_or_create_calendar_layer, update_calendar_event, update_student_event_attendance,
    update_timeslot_dates_from_calendar,
)
from lms.calendars.tests.factories import CalendarEventFactory, CalendarLayerFactory
from lms.classrooms.tests.factories import StudentSlotFactory, TimeslotFactory
from lms.contrib.calendar.settings import CALENDAR_ROBOT_UID
from lms.courses.tests.factories import CourseFactory


class GetOrCreateCalendarLayerTestCase(TestCase):
    def test_already_existing_calendar(self):
        layer = CalendarLayerFactory()

        result = get_or_create_calendar_layer(course_id=layer.course_id)

        self.assertEqual(result.id, layer.id)

    def test_no_layer_id(self):
        course = CourseFactory()

        with self.assertRaises(CalendarException):
            get_or_create_calendar_layer(course_id=course.id)

    def test_create_new_layer(self):
        course = CourseFactory()

        calendar_api = MagicMock()
        calendar_api.create_layer.post.return_value = {"layerId": 123}

        with patch('lms.calendars.services.calendar_api', new=calendar_api):
            result = get_or_create_calendar_layer(course_id=course.id)
            self.assertEqual(result.id, 123)


class GetOrCreateCalendarEventTestCase(TestCase):
    def test_already_existing_event(self):
        event = CalendarEventFactory()

        result = get_or_create_calendar_event(timeslot_id=event.timeslot_id)

        self.assertEqual(result.id, event.id)

    def test_no_event_id(self):
        calendar = CalendarLayerFactory()
        timeslot = TimeslotFactory(course=calendar.course)

        get_or_create_calendar_layer_mock = MagicMock()
        get_or_create_calendar_layer_mock.return_value = calendar

        with patch('lms.calendars.services.get_or_create_calendar_layer', new=get_or_create_calendar_layer_mock):
            with self.assertRaises(CalendarException):
                get_or_create_calendar_event(timeslot_id=timeslot.id)

    def test_create_new_event(self):
        calendar = CalendarLayerFactory()
        timeslot = TimeslotFactory(course=calendar.course)
        expected_uid = 'test-uid-1337'

        expected_payload = {
            "layerId": calendar.id,
            "name": timeslot.classroom.name,
            "type": "learning",
            "availability": "busy",
            "startTs": timeslot.begin_date.replace(microsecond=0).isoformat(),
            "endTs": timeslot.end_date.replace(microsecond=0).isoformat(),
            "description": timeslot.summary,
            "attendees": [],
            "othersCanView": True,
        }

        get_or_create_calendar_layer_mock = MagicMock()
        get_or_create_calendar_layer_mock.return_value = calendar

        with patch('lms.calendars.services.get_or_create_calendar_layer', new=get_or_create_calendar_layer_mock):
            calendar_api = MagicMock()
            calendar_api.create_event.post.return_value = {"showEventId": 123}

            with patch(
                'lms.calendars.services.CALENDAR_ROBOT_UID', new=expected_uid
            ), patch(
                'lms.calendars.services.calendar_api', new=calendar_api
            ):
                result = get_or_create_calendar_event(timeslot_id=timeslot.id)
                self.assertEqual(result.id, 123)
                calendar_api.create_event.post.assert_called_once_with(json=expected_payload, uid=expected_uid)


class UpdateEventAttendeesTestCase(TestCase):
    CALENDAR_API = 'lms.calendars.services.calendar_api'
    UPDATE_TIMESLOT_DATES = 'lms.calendars.services._update_timeslot_dates'

    def setUp(self):
        self.event = CalendarEventFactory()
        self.student_slots = StudentSlotFactory.create_batch(10, timeslot=self.event.timeslot)
        self.student_slot = self.student_slots[5]
        self.emails = [student_slot.student.user.get_staff_email() for student_slot in self.student_slots]
        self.expected_begin_date = (self.event.timeslot.begin_date + timedelta(hours=8)).isoformat()
        self.expected_end_date = (self.event.timeslot.begin_date + timedelta(hours=8)).isoformat()
        self.calendar_api = MagicMock()
        self.calendar_api.update_event.post = MagicMock()

    def test_no_event(self):
        timeslot = TimeslotFactory()
        student = StudentSlotFactory().student

        calendar_api = MagicMock()
        calendar_api.update_event.post = MagicMock()

        with patch('lms.calendars.services.calendar_api', new=calendar_api):
            update_student_event_attendance(timeslot_id=timeslot.id, student_id=student.id)

            calendar_api.update_event.post.assert_not_called()

    @override_settings(
        CALENDAR_API_RESPONSE_STATUS_FIELD='calendar_status',
        CALENDAR_API_RESPONSE_SUCCESS_STATUS='calendar_ok',
        CALENDAR_TIMEZONE='America/New_York',
    )
    def test_update_event_attendees(self):
        self.calendar_api.update_event.post.return_value = {'calendar_status': 'calendar_ok'}
        self.calendar_api.get_event.get.return_value = {
            "startTs": self.expected_begin_date,
            "endTs": self.expected_end_date,
            "attendees": [
                {"email": self.emails[1]},
                {"email": self.emails[2]},
                {"email": self.emails[3]},
                {"email": self.emails[4]},
            ],
            'optionalAttendees': [
                {"email": self.emails[0]},
                {"email": self.emails[5]},
            ],
            'resources': [
                {"email": self.emails[6]},
                {"email": self.emails[7]},
                {"email": self.emails[8]},
                {"email": self.emails[9]},
            ]
        }

        expected_attendees = sorted(self.emails[1:])
        expected_optional_attendees = [self.emails[0]]

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            update_student_event_attendance(timeslot_id=self.event.timeslot_id, student_id=self.student_slot.student_id)

            self.calendar_api.update_event.post.assert_called_once_with(
                json={
                    "attendees": expected_attendees,
                    "optionalAttendees": expected_optional_attendees,
                    "description": self.event.timeslot.summary,
                    "startTs": self.expected_begin_date,
                    "endTs": self.expected_end_date,
                },
                uid=CALENDAR_ROBOT_UID,
                id=self.event.id,
                tz='America/New_York',
            )
            mock.assert_called_once_with(
                timeslot=self.event.timeslot,
                begin_date=self.expected_begin_date, end_date=self.expected_end_date,
            )

    def test_update_event_attendees_invalid_event_payload(self):
        self.calendar_api.get_event.get.return_value = {
            "startTs": None,
            "endTs": None,
            'optionalAttendees': [],
            'resources': []
        }

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with self.assertRaises(CalendarException):
                update_student_event_attendance(
                    timeslot_id=self.event.timeslot_id, student_id=self.student_slot.student_id
                )

            self.calendar_api.update_event.post.assert_not_called()
            mock.assert_not_called()

    def test_update_event_attendees_invalid_update_status(self):
        self.calendar_api.delete_event.post.return_value = {'error': 'unknown_error'}
        self.calendar_api.get_event.get.return_value = {
            "startTs": None,
            "endTs": None,
            'attendees': [],
            'optionalAttendees': [],
            'resources': []
        }

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with self.assertRaises(CalendarException):
                update_student_event_attendance(
                    timeslot_id=self.event.timeslot_id, student_id=self.student_slot.student_id
                )

            self.calendar_api.update_event.post.assert_called_once_with(
                json={
                    "attendees": [self.emails[5]],
                    "optionalAttendees": [],
                    "description": self.event.timeslot.summary,
                    "startTs": None,
                    "endTs": None,
                },
                uid=CALENDAR_ROBOT_UID,
                id=self.event.id,
                tz=settings.CALENDAR_TIMEZONE,
            )
            mock.assert_called_once_with(timeslot=self.event.timeslot, begin_date=None, end_date=None)


class UpdateCalendarEventTestCase(TestCase):
    CALENDAR_API = 'lms.calendars.services.calendar_api'
    UPDATE_TIMESLOT_DATES = 'lms.calendars.services._update_timeslot_dates'

    def setUp(self):
        self.event = CalendarEventFactory()
        self.student_slots = StudentSlotFactory.create_batch(3, timeslot=self.event.timeslot)
        self.emails = [student_slot.student.user.get_staff_email() for student_slot in self.student_slots]
        self.expected_attendees = sorted(self.emails[:2])
        self.expected_optional_attendees = [self.emails[-1]]
        self.expected_begin_date = (self.event.timeslot.begin_date + timedelta(hours=8)).isoformat()
        self.expected_end_date = (self.event.timeslot.begin_date + timedelta(hours=8)).isoformat()
        self.calendar_api = MagicMock()
        self.calendar_api.update_event.post = MagicMock()

    def test_no_event(self):
        timeslot = TimeslotFactory()

        with patch(self.CALENDAR_API, new=self.calendar_api):
            update_calendar_event(timeslot_id=timeslot.id)

            self.calendar_api.update_event.post.assert_not_called()

    @override_settings(
        CALENDAR_API_RESPONSE_STATUS_FIELD='calendar_status',
        CALENDAR_API_RESPONSE_SUCCESS_STATUS='calendar_ok',
        CALENDAR_TIMEZONE='America/New_York',
    )
    def test_update_calendar_event(self):
        self.calendar_api.update_event.post.return_value = {'calendar_status': 'calendar_ok'}
        self.calendar_api.get_event.get.return_value = {
            "startTs": self.expected_begin_date,
            "endTs": self.expected_end_date,
            "attendees": [
                {"email": self.emails[0]},
            ],
            'optionalAttendees': [
                {"email": self.emails[2]},
            ],
            'resources': [
                {"email": self.emails[1]},
            ]
        }

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            update_calendar_event(timeslot_id=self.event.timeslot_id)

            self.calendar_api.update_event.post.assert_called_once_with(
                json={
                    "attendees": self.expected_attendees,
                    "optionalAttendees": self.expected_optional_attendees,
                    "description": self.event.timeslot.summary,
                    "startTs": self.expected_begin_date,
                    "endTs": self.expected_end_date,
                },
                uid=CALENDAR_ROBOT_UID,
                id=self.event.id,
                tz='America/New_York',
            )
            mock.assert_called_once_with(
                timeslot=self.event.timeslot, begin_date=self.expected_begin_date, end_date=self.expected_end_date,
            )

    def test_update_event_attendees_invalid_event_payload(self):
        self.calendar_api.get_event.get.return_value = {
            "startTs": None,
            "endTs": None,
            'optionalAttendees': [],
            'resources': []
        }

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with self.assertRaises(CalendarException):
                update_calendar_event(timeslot_id=self.event.timeslot_id)

            self.calendar_api.update_event.post.assert_not_called()
            mock.assert_not_called()

    def test_update_event_attendees_invalid_update_status(self):
        self.calendar_api.delete_event.post.return_value = {'error': 'unknown_error'}
        self.calendar_api.get_event.get.return_value = {
            "startTs": None,
            "endTs": None,
            'attendees': [],
            'optionalAttendees': [],
            'resources': []
        }

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with self.assertRaises(CalendarException):
                update_calendar_event(timeslot_id=self.event.timeslot_id)

            self.calendar_api.update_event.post.assert_called_once_with(
                json={
                    "attendees": [],
                    "optionalAttendees": [],
                    "description": self.event.timeslot.summary,
                    "startTs": None,
                    "endTs": None,
                },
                uid=CALENDAR_ROBOT_UID,
                id=self.event.id,
                tz=settings.CALENDAR_TIMEZONE,
            )
            mock.assert_called_once_with(timeslot=self.event.timeslot, begin_date=None, end_date=None)


class DeleteCalendarEventTestCase(TestCase):
    EXPECTED_UID = 'test-uid-1337'
    ROBOT_UID = 'lms.calendars.services.CALENDAR_ROBOT_UID'
    CALENDAR_API = 'lms.calendars.services.calendar_api'

    def setUp(self):
        self.calendar_api = MagicMock()
        self.calendar_api.delete_event.post = MagicMock()

    @override_settings(
        CALENDAR_API_RESPONSE_STATUS_FIELD='calendar_status',
        CALENDAR_API_RESPONSE_SUCCESS_STATUS='calendar_ok'
    )
    def test_delete_calendar_event_success(self):
        with patch(self.ROBOT_UID, new=self.EXPECTED_UID):
            self.calendar_api.delete_event.post.return_value = {'calendar_status': 'calendar_ok'}
            expected_id = 1337

            with patch(self.CALENDAR_API, new=self.calendar_api):
                delete_calendar_event(event_id=expected_id)
                self.calendar_api.delete_event.post.assert_called_once_with(uid=self.EXPECTED_UID, id=expected_id)

    def test_delete_calendar_event_failure(self):
        with patch(self.ROBOT_UID, new=self.EXPECTED_UID):
            self.calendar_api.delete_event.post.return_value = {'error': 'unknown_error'}
            expected_id = 27

            with patch(self.CALENDAR_API, new=self.calendar_api):
                with self.assertRaises(CalendarException):
                    delete_calendar_event(event_id=expected_id)
                self.calendar_api.delete_event.post.assert_called_once_with(uid=self.EXPECTED_UID, id=expected_id)


class UpdateTimeslotDatesTestCase(TestCase):
    BEGIN_DATE = '2020-07-26T01:23:45'
    END_DATE = '2020-07-26T05:00:00'

    def setUp(self):
        self.timeslot = TimeslotFactory()

    def test_update_timeslot_dates_no_begin_date(self):
        with self.assertNumQueries(0):
            _update_timeslot_dates(timeslot=self.timeslot, begin_date=None, end_date=self.END_DATE)

    def test_update_timeslot_dates_invalid_begin_date(self):
        with self.assertNumQueries(0):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=f'{self.BEGIN_DATE}-invalid', end_date=self.END_DATE
            )

    def test_update_timeslot_dates_no_end_date(self):
        with self.assertNumQueries(0):
            _update_timeslot_dates(timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=None)

    def test_update_timeslot_dates_invalid_end_date(self):
        with self.assertNumQueries(0):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=f'{self.END_DATE}-invalid'
            )

    def test_update_timeslot_dates(self):
        with self.assertNumQueries(2):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=self.END_DATE
            )
        with self.assertNumQueries(0):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=self.END_DATE
            )

    @override_settings(
        CALENDAR_TIMEZONE='America/New_York',
    )
    def test_update_timeslot_dates_different_timezones(self):
        self.timeslot.begin_date = parse_datetime(self.BEGIN_DATE).replace(tzinfo=pytz.timezone('Etc/UTC'))
        self.timeslot.end_date = parse_datetime(self.END_DATE).replace(tzinfo=pytz.timezone('Etc/UTC'))
        self.timeslot.save()

        with self.assertNumQueries(2):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=self.END_DATE
            )
        with self.assertNumQueries(0):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=self.END_DATE
            )

    @override_settings(
        CALENDAR_TIMEZONE='America/New_York',
    )
    def test_update_timeslot_dates_same_timezones(self):
        self.timeslot.begin_date = parse_datetime(self.BEGIN_DATE).replace(tzinfo=pytz.timezone('America/New_York'))
        self.timeslot.end_date = parse_datetime(self.END_DATE).replace(tzinfo=pytz.timezone('America/New_York'))
        self.timeslot.save()

        with self.assertNumQueries(0):
            _update_timeslot_dates(
                timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=self.END_DATE
            )


class UpdateTimeslotDatesFromCalendarTestCase(TestCase):
    EXPECTED_UID = 'test-uid-1337'
    ROBOT_UID = 'lms.calendars.services.CALENDAR_ROBOT_UID'
    CALENDAR_API = 'lms.calendars.services.calendar_api'
    UPDATE_TIMESLOT_DATES = 'lms.calendars.services._update_timeslot_dates'
    BEGIN_DATE = '2020-07-26T01:23:45'
    END_DATE = '2020-07-26T05:00:00'

    def setUp(self):
        self.event = CalendarEventFactory()
        self.timeslot = self.event.timeslot
        self.calendar_api = MagicMock()
        self.calendar_api.get_event.get = MagicMock()

    def test_no_event(self):
        timeslot = TimeslotFactory()

        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            update_timeslot_dates_from_calendar(timeslot_id=timeslot.id)

        self.calendar_api.get_event.get.assert_not_called()
        mock.assert_not_called()

    def test_update_timeslot_dates_from_calendar_no_start_date(self):
        self.calendar_api.get_event.get.return_value = {'endTs': self.END_DATE}
        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with self.assertRaises(CalendarException), patch(self.ROBOT_UID, new=self.EXPECTED_UID):
                update_timeslot_dates_from_calendar(timeslot_id=self.timeslot.id)
            self.calendar_api.get_event.get.assert_called_once_with(
                uid=self.EXPECTED_UID, eventId=self.event.id, tz=settings.CALENDAR_TIMEZONE,
            )
            mock.assert_not_called()

    def test_update_timeslot_dates_from_calendar_no_end_date(self):
        self.calendar_api.get_event.get.return_value = {'startTs': self.BEGIN_DATE}
        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with self.assertRaises(CalendarException), patch(self.ROBOT_UID, new=self.EXPECTED_UID):
                update_timeslot_dates_from_calendar(timeslot_id=self.timeslot.id)
            self.calendar_api.get_event.get.assert_called_once_with(
                uid=self.EXPECTED_UID, eventId=self.event.id, tz=settings.CALENDAR_TIMEZONE,
            )
            mock.assert_not_called()

    def test_update_timeslot_dates_from_calendar(self):
        self.calendar_api.get_event.get.return_value = {'startTs': self.BEGIN_DATE, 'endTs': self.END_DATE}
        with patch(self.CALENDAR_API, new=self.calendar_api), patch(self.UPDATE_TIMESLOT_DATES) as mock:
            with patch(self.ROBOT_UID, new=self.EXPECTED_UID):
                update_timeslot_dates_from_calendar(timeslot_id=self.timeslot.id)
            self.calendar_api.get_event.get.assert_called_once_with(
                uid=self.EXPECTED_UID, eventId=self.event.id, tz=settings.CALENDAR_TIMEZONE,
            )
            mock.assert_called_once_with(timeslot=self.timeslot, begin_date=self.BEGIN_DATE, end_date=self.END_DATE)
