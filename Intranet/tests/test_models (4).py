from datetime import timedelta
from unittest.mock import MagicMock, patch

import faker

from django.core.exceptions import ValidationError
from django.test import TestCase, TransactionTestCase
from django.utils.timezone import utc

from lms.courses.models import CourseStudent
from lms.courses.tests.factories import CourseFactory, CourseGroupFactory, CourseStudentFactory
from lms.users.tests.factories import UserFactory

from ..models import StudentSlot, Timeslot, TimeslotExchange
from .factories import ClassroomFactory, StudentSlotFactory, TimeslotExchangeFactory, TimeslotFactory

fake = faker.Faker()


class TimeslotDatesModelTestCase(TestCase):
    def test_dates(self):
        date = fake.date_time(tzinfo=utc)

        TimeslotFactory(begin_date=date)

        TimeslotFactory(begin_date=date, end_date=date + timedelta(hours=1))

        with self.assertRaises(ValidationError):
            TimeslotFactory(begin_date=date, end_date=date - timedelta(hours=1))


class TimeslotNumParticipantsModelTestCase(TestCase):
    def setUp(self) -> None:
        self.course = CourseFactory(is_active=True)

    def assert_reject(self, student_slot, timeslot, expected_num_participants):
        reject_reason = fake.pystr()
        student_slot.reject(reject_reason)

        timeslot.refresh_from_db()
        self.assertEqual(timeslot.num_participants, expected_num_participants)
        self.assertEqual(student_slot.history.all()[0].history_change_reason, reject_reason)

    def assert_cancel(self, student_slot, timeslot, expected_num_participants):
        cancel_reason = fake.pystr()
        student_slot.cancel(cancel_reason)

        timeslot.refresh_from_db()
        self.assertEqual(timeslot.num_participants, expected_num_participants)
        self.assertEqual(student_slot.history.all()[0].history_change_reason, cancel_reason)

    def assert_accept(self, student_slot, timeslot, expected_num_participants):
        accept_reason = fake.pystr()
        student_slot.accept(accept_reason)

        timeslot.refresh_from_db()
        self.assertEqual(timeslot.num_participants, expected_num_participants)
        self.assertEqual(student_slot.history.all()[0].history_change_reason, accept_reason)

    def assert_change(self, student_slot, timeslot, expected_num_participants):
        change_reason = fake.pystr()
        student_slot.exchange(change_reason)

        timeslot.refresh_from_db()
        self.assertEqual(timeslot.num_participants, expected_num_participants)
        self.assertEqual(student_slot.history.all()[0].history_change_reason, change_reason)

    def test_num_participants_when_student_slot_updated(self):
        classroom = ClassroomFactory(course=self.course)
        timeslot = TimeslotFactory(classroom=classroom, max_participants=3)
        students = CourseStudentFactory.create_batch(3, course=self.course)
        student_slots = [
            StudentSlotFactory(timeslot=timeslot, student=student)
            for student in students
        ]

        timeslot.refresh_from_db()
        self.assertEqual(timeslot.num_participants, 3)

        self.assert_reject(student_slots[0], timeslot, 2)
        self.assert_cancel(student_slots[0], timeslot, 2)
        self.assert_change(student_slots[0], timeslot, 2)

        self.assert_accept(student_slots[0], timeslot, 3)
        self.assert_cancel(student_slots[0], timeslot, 2)

        self.assert_accept(student_slots[0], timeslot, 3)
        self.assert_change(student_slots[0], timeslot, 2)

    def test_num_participants_when_timeslot_updated(self):
        classroom = ClassroomFactory(course=self.course)
        timeslot = TimeslotFactory(classroom=classroom, max_participants=3)
        students = CourseStudentFactory.create_batch(3, course=self.course)
        student_slots = [
            StudentSlotFactory(timeslot=timeslot, student=student)
            for student in students
        ]

        timeslot.save()
        self.assertEqual(timeslot.num_participants, 3)

        student_slots[0].cancel()
        timeslot.save()
        self.assertEqual(timeslot.num_participants, 2)

    def test_available_for_user(self):
        user = UserFactory()
        groups = CourseGroupFactory.create_batch(3, course=self.course)
        classroom = ClassroomFactory(course=self.course)
        timeslots = [
            TimeslotFactory.create_batch(3, classroom=classroom, course_groups=[group], max_participants=0)
            for group in groups
        ]
        self.active_student = CourseStudentFactory(
            course=self.course, group=groups[0], user=user, status=CourseStudent.StatusChoices.ACTIVE,
        )
        self.inactive_student = CourseStudentFactory(
            course=self.course, group=groups[1], user=user, status=CourseStudent.StatusChoices.EXPELLED,
        )

        available_timeslots = sorted(timeslots[0], key=lambda t: t.created, reverse=True)
        available_timeslots = sorted(available_timeslots, key=lambda t: t.begin_date)
        available_timeslots = sorted(available_timeslots, key=lambda t: t.classroom_id)

        self.assertListEqual(
            list(Timeslot.objects.available_for(user=user)),
            available_timeslots,
        )

    def test_checkout_slot_inactive_student(self):
        user = UserFactory()
        student = CourseStudentFactory(course=self.course, user=user, status=CourseStudent.StatusChoices.ACTIVE)
        classroom = ClassroomFactory(course=self.course)
        timeslot = TimeslotFactory(classroom=classroom, max_participants=0)
        student_slot = StudentSlotFactory(student=student, timeslot=timeslot)

        student.status = CourseStudent.StatusChoices.EXPELLED
        with self.assertRaises(ValidationError):
            student_slot.save()

        student_slot.status = StudentSlot.StatusChoices.REJECTED
        student_slot.save()

    def test_simultaneous_timeslot_enrollment(self):
        classroom = ClassroomFactory(course=self.course)
        timeslot = TimeslotFactory(classroom=classroom, max_participants=2)

        with self.assertRaises(ValidationError):
            StudentSlotFactory.create_batch(5, timeslot=timeslot)

        timeslot.refresh_from_db()
        self.assertEqual(timeslot.num_participants, 2)


class TimeslotExchangeDeleteTestCase(TestCase):
    def test_delete_when_canceled(self):
        exchange = TimeslotExchangeFactory()

        exchange.student_slot.status = StudentSlot.StatusChoices.CANCELED
        exchange.student_slot.save()

        self.assertFalse(
            TimeslotExchange.objects.filter(student_slot_id=exchange.student_slot_id).exists()
        )

    def test_delete_when_rejected(self):
        exchange = TimeslotExchangeFactory()

        exchange.student_slot.status = StudentSlot.StatusChoices.REJECTED
        exchange.student_slot.save()

        self.assertFalse(
            TimeslotExchange.objects.filter(student_slot_id=exchange.student_slot_id).exists()
        )

    def test_delete_when_exchanged(self):
        exchange = TimeslotExchangeFactory()

        exchange.student_slot.status = StudentSlot.StatusChoices.EXCHANGED
        exchange.student_slot.save()

        self.assertTrue(
            TimeslotExchange.objects.filter(student_slot_id=exchange.student_slot_id).exists()
        )

    def test_delete_accepted_exchange(self):
        exchange = TimeslotExchangeFactory(is_active=False, student_slot__status=StudentSlot.StatusChoices.EXCHANGED)

        exchange.student_slot.status = StudentSlot.StatusChoices.CANCELED
        exchange.student_slot.save()

        self.assertTrue(
            TimeslotExchange.objects.filter(student_slot_id=exchange.student_slot_id).exists()
        )


class CourseStudentExpellFromSlotsModelTestCase(TransactionTestCase):
    def test_expell(self) -> None:
        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.classrooms.services.timezone.now', new=mocked_now):
            course = CourseFactory(is_active=True)
            course_student = CourseStudentFactory(course=course, status=CourseStudent.StatusChoices.ACTIVE)

            accepted_timeslot_in_future = TimeslotFactory(
                classroom__course=course, max_participants=0, begin_date=now_moment + timedelta(days=1),
            )
            accepted_studentslot_in_future = StudentSlotFactory(
                timeslot=accepted_timeslot_in_future, student=course_student, status=StudentSlot.StatusChoices.ACCEPTED,
            )
            StudentSlotFactory.create_batch(
                5, timeslot=accepted_timeslot_in_future, student__course=course,
                student__status=CourseStudent.StatusChoices.ACTIVE, status=StudentSlot.StatusChoices.ACCEPTED,
            )

            accepted_timeslot_in_past = TimeslotFactory(
                classroom__course=course, max_participants=0, begin_date=now_moment - timedelta(days=1),
            )
            accepted_student_slot_in_past = StudentSlotFactory(
                timeslot=accepted_timeslot_in_past, student=course_student, status=StudentSlot.StatusChoices.ACCEPTED,
            )
            StudentSlotFactory.create_batch(
                5, timeslot=accepted_timeslot_in_past, student__course=course,
                student__status=CourseStudent.StatusChoices.ACTIVE, status=StudentSlot.StatusChoices.ACCEPTED,
            )

            rejected_timeslot_in_future = TimeslotFactory(
                classroom__course=course, max_participants=0, begin_date=now_moment + timedelta(days=1),
            )
            rejected_student_slot_in_future = StudentSlotFactory(
                timeslot=rejected_timeslot_in_future, student=course_student, status=StudentSlot.StatusChoices.REJECTED,
            )
            StudentSlotFactory.create_batch(
                5, timeslot=rejected_timeslot_in_future, student__course=course,
                student__status=CourseStudent.StatusChoices.ACTIVE, status=StudentSlot.StatusChoices.ACCEPTED,
            )

            rejected_timeslot_in_past = TimeslotFactory(
                classroom__course=course, max_participants=0, begin_date=now_moment - timedelta(days=1),
            )
            rejected_student_slot_in_past = StudentSlotFactory(
                timeslot=rejected_timeslot_in_past, student=course_student, status=StudentSlot.StatusChoices.REJECTED,
            )
            StudentSlotFactory.create_batch(
                5, timeslot=rejected_timeslot_in_past, student__course=course,
                student__status=CourseStudent.StatusChoices.ACTIVE, status=StudentSlot.StatusChoices.ACCEPTED,
            )

            accepted_timeslot_in_future.refresh_from_db()
            self.assertEqual(accepted_timeslot_in_future.num_participants, 6)

            accepted_timeslot_in_past.refresh_from_db()
            self.assertEqual(accepted_timeslot_in_past.num_participants, 6)

            rejected_timeslot_in_future.refresh_from_db()
            self.assertEqual(rejected_timeslot_in_future.num_participants, 5)

            rejected_timeslot_in_past.refresh_from_db()
            self.assertEqual(rejected_timeslot_in_past.num_participants, 5)

            with patch('lms.classrooms.services.update_student_event_attendance_task.delay') as mock:
                with self.assertNumQueries(7):
                    course_student.expell()
                mock.assert_called_once_with(timeslot_id=accepted_timeslot_in_future.id, student_id=course_student.id)

            accepted_timeslot_in_future.refresh_from_db()
            self.assertEqual(accepted_timeslot_in_future.num_participants, 5)
            accepted_studentslot_in_future.refresh_from_db()
            self.assertEqual(accepted_studentslot_in_future.status, StudentSlot.StatusChoices.REJECTED)

            accepted_timeslot_in_past.refresh_from_db()
            self.assertEqual(accepted_timeslot_in_past.num_participants, 6)
            accepted_student_slot_in_past.refresh_from_db()
            self.assertEqual(accepted_student_slot_in_past.status, StudentSlot.StatusChoices.ACCEPTED)

            rejected_timeslot_in_future.refresh_from_db()
            self.assertEqual(rejected_timeslot_in_future.num_participants, 5)
            rejected_student_slot_in_future.refresh_from_db()
            self.assertEqual(rejected_student_slot_in_future.status, StudentSlot.StatusChoices.REJECTED)

            rejected_timeslot_in_past.refresh_from_db()
            self.assertEqual(rejected_timeslot_in_past.num_participants, 5)
            rejected_student_slot_in_past.refresh_from_db()
            self.assertEqual(rejected_student_slot_in_past.status, StudentSlot.StatusChoices.REJECTED)
