from datetime import timedelta
from unittest.mock import patch

import faker

from django.test import TransactionTestCase
from django.utils.timezone import utc

from lms.classrooms.models import StudentSlot
from lms.classrooms.tests.factories import ClassroomFactory, StudentSlotFactory, TimeslotFactory

from .factories import CalendarEventFactory

fake = faker.Faker()


class ClassroomPostSaveHandlerTestCase(TransactionTestCase):
    def test_classroom_post_save(self):
        with patch('lms.calendars.receivers.create_layer_for_course_task.delay') as mock:
            ClassroomFactory()
            mock.assert_not_called()

            classroom = ClassroomFactory(calendar_enabled=True)
            mock.assert_called_once_with(course_id=classroom.course_id)

            classroom.save()
            mock.assert_called_once_with(course_id=classroom.course_id)


class TimeslotPostSaveHandlerTestCase(TransactionTestCase):
    CREATE_LAYER_TASK = 'lms.calendars.receivers.create_layer_for_course_task.delay'
    CREATE_EVENT_TASK = 'lms.calendars.receivers.create_event_for_timeslot_task.delay'
    UPDATE_EVENT_TASK = 'lms.calendars.receivers.update_event_for_timeslot_task.delay'
    UPDATE_DATES_TASK = 'lms.calendars.receivers.update_timeslot_dates_from_calendar_task.delay'

    def test_timeslot_post_save_create(self):
        with patch(self.CREATE_EVENT_TASK) as create_mock, patch(self.UPDATE_DATES_TASK) as dates_mock, patch(
            self.UPDATE_EVENT_TASK
        ) as update_mock:
            TimeslotFactory()
            create_mock.assert_not_called()
            dates_mock.assert_not_called()
            update_mock.assert_not_called()

            with patch(self.CREATE_LAYER_TASK):
                timeslot = TimeslotFactory(classroom__calendar_enabled=True)
                create_mock.assert_called_once_with(timeslot_id=timeslot.id)
                dates_mock.assert_not_called()
                update_mock.assert_not_called()

                create_mock.reset_mock()
                timeslot.save()
                create_mock.assert_not_called()
                dates_mock.assert_not_called()
                update_mock.assert_not_called()

    def test_timeslot_post_save_modify_calendar_disabled(self):
        with patch(self.UPDATE_DATES_TASK) as mock, patch(self.CREATE_LAYER_TASK):
            timeslot = TimeslotFactory()
            mock.assert_not_called()

            timeslot.begin_date = timeslot.begin_date + timedelta(hours=8)
            timeslot.end_date = timeslot.end_date + timedelta(hours=8)
            timeslot.save()
            mock.assert_not_called()

    def test_timeslot_post_save_calendar_enabled_modify_summary(self):
        with patch(self.CREATE_EVENT_TASK), patch(self.CREATE_LAYER_TASK), patch(
            self.UPDATE_DATES_TASK
        ) as dates_mock, patch(self.UPDATE_EVENT_TASK) as update_mock:
            timeslot = TimeslotFactory(classroom__calendar_enabled=True)

            timeslot.summary = f'{timeslot.summary}-modified'
            timeslot.save()
            dates_mock.assert_not_called()
            update_mock.assert_called_once_with(timeslot_id=timeslot.id)

    def test_timeslot_post_save_calendar_enabled_modify_begin_date(self):
        with patch(self.CREATE_EVENT_TASK), patch(self.CREATE_LAYER_TASK), patch(
            self.UPDATE_DATES_TASK
        ) as dates_mock, patch(self.UPDATE_EVENT_TASK) as update_mock:
            now_moment = fake.date_time(tzinfo=utc)
            timeslot = TimeslotFactory(classroom__calendar_enabled=True,
                                       begin_date=now_moment,
                                       end_date=now_moment + timedelta(hours=4))

            timeslot.begin_date += timedelta(hours=1)
            timeslot.save()
            dates_mock.assert_called_once_with(timeslot_id=timeslot.id)
            update_mock.assert_not_called()

    def test_timeslot_post_save_calendar_enabled_modify_end_date(self):
        with patch(self.CREATE_EVENT_TASK), patch(self.CREATE_LAYER_TASK), patch(
            self.UPDATE_DATES_TASK
        ) as dates_mock, patch(self.UPDATE_EVENT_TASK) as update_mock:
            timeslot = TimeslotFactory(classroom__calendar_enabled=True)

            timeslot.end_date += timedelta(hours=1)
            timeslot.save()
            dates_mock.assert_called_once_with(timeslot_id=timeslot.id)
            update_mock.assert_not_called()

    def test_timeslot_post_save_calendar_enabled_modify_summary_and_date(self):
        with patch(self.CREATE_EVENT_TASK), patch(self.CREATE_LAYER_TASK), patch(
            self.UPDATE_DATES_TASK
        ) as dates_mock, patch(self.UPDATE_EVENT_TASK) as update_mock:
            timeslot = TimeslotFactory(classroom__calendar_enabled=True)

            timeslot.summary = f'{timeslot.summary}-modified'
            timeslot.end_date += timedelta(hours=1)
            timeslot.save()
            dates_mock.assert_called_once_with(timeslot_id=timeslot.id)
            update_mock.assert_called_once_with(timeslot_id=timeslot.id)

    def test_timeslot_post_save_calendar_enabled_modify_other(self):
        with patch(self.CREATE_EVENT_TASK), patch(self.CREATE_LAYER_TASK), patch(
            self.UPDATE_DATES_TASK
        ) as dates_mock, patch(self.UPDATE_EVENT_TASK) as update_mock:
            timeslot = TimeslotFactory(classroom__calendar_enabled=True)

            timeslot.title = f'{timeslot.title}-modified'
            timeslot.save()
            dates_mock.assert_not_called()
            update_mock.assert_not_called()


class StudentSlotPostSaveHandlerTestCase(TransactionTestCase):
    def test_student_slot_post_save(self):
        with patch('lms.calendars.receivers.update_student_event_attendance_task.delay') as mock:
            student_slot = StudentSlotFactory()
            mock.assert_called_once_with(timeslot_id=student_slot.timeslot_id, student_id=student_slot.student_id)
            mock.reset_mock()

            student_slot.save()
            mock.assert_not_called()

            student_slot.status = StudentSlot.StatusChoices.REJECTED
            student_slot.save()
            mock.assert_called_once_with(timeslot_id=student_slot.timeslot_id, student_id=student_slot.student_id)


class CalendarEventPostDeleteHandlerTestCase(TransactionTestCase):
    def setUp(self):
        self.calendar_event = CalendarEventFactory.create_batch(5)[-1]

    def test_delete_calendar_event(self):
        expected_id = self.calendar_event.id

        with patch('lms.calendars.services.delete_calendar_event') as mock:
            self.calendar_event.delete()
            mock.assert_called_once_with(event_id=expected_id)
