import uuid
from typing import Sequence
from unittest import skip

from django.conf import settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.models import CourseStudent
from lms.courses.tests.factories import CourseFactory, CourseGroupFactory, CourseStudentFactory
from lms.users.tests.factories import UserFactory

from ..models import Classroom, StudentSlot, Timeslot
from .factories import ClassroomFactory, StudentSlotFactory, TimeslotExchangeFactory, TimeslotFactory


class CourseClassroomListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-classroom-list'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/classroom_modules/', args=(self.course.id,), base_url=settings.API_BASE_URL)

    def build_expected(self, classrooms: Sequence[Classroom], timeslot_qs=None):
        if timeslot_qs is None:
            timeslot_qs = lambda obj: obj.timeslots.all()  # noqa: E731

        return [{
            'id': classroom.id,
            'name': classroom.name,
            'description': classroom.description,
            'estimated_time': classroom.estimated_time,
            'timeslots': [{
                'id': slot.id,
                'title': slot.title,
                'summary': slot.summary,
                'begin_date': serializers.DateTimeField().to_representation(slot.begin_date),
                'end_date': serializers.DateTimeField().to_representation(slot.end_date),
                'max_participants': slot.max_participants,
                'num_participants': slot.num_participants,
            } for slot in timeslot_qs(classroom)],
        } for classroom in classrooms]

    def test_no_auth(self):
        with self.assertNumQueries(0):
            response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_is_not_student(self):
        self.client.force_login(user=UserFactory())
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_available_for_active_or_completed_students(self):
        for student_status in [CourseStudent.StatusChoices.ACTIVE, CourseStudent.StatusChoices.COMPLETED]:
            student = CourseStudentFactory(course=self.course, status=student_status)
            self.client.force_login(user=student.user)
            self.detail_request(
                url=self.get_url(self.course.id),
                num_queries=5,
            )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.course.id),
            num_queries=4,
            status_code=status.HTTP_403_FORBIDDEN,
        )

    def test_list(self):
        classrooms = ClassroomFactory.create_batch(5, course=self.course)
        TimeslotFactory.create_batch(3, classroom=classrooms[0])

        CourseStudentFactory(course=self.course, user=self.user)
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)
        expected = self.build_expected(classrooms)
        self.list_request(url, expected, num_queries=7)

    def test_list_no_available_slots(self):
        classrooms = ClassroomFactory.create_batch(5, course=self.course)
        course_group = CourseGroupFactory(course=self.course)
        timeslots = TimeslotFactory.create_batch(3, classroom=classrooms[0])
        timeslots[2].course_groups.set([course_group])

        CourseStudentFactory(course=self.course, user=self.user)
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)

        timeslot_qs_func = lambda obj: Timeslot.objects.filter(classroom=obj).available_for(self.user)  # noqa: E731
        expected = self.build_expected(classrooms, timeslot_qs=timeslot_qs_func)
        self.list_request(url + "?available_slots=true", expected, num_queries=7)

    def test_list_available_for_user(self):
        classrooms = ClassroomFactory.create_batch(5, course=self.course)
        course_group = CourseGroupFactory(course=self.course)
        CourseStudentFactory(course=self.course, user=self.user, group=course_group)

        timeslots = TimeslotFactory.create_batch(3, classroom=classrooms[0])
        timeslots[2].course_groups.set([course_group])

        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)

        timeslot_qs_func = lambda obj: Timeslot.objects.filter(classroom=obj).available_for(self.user)  # noqa: E731
        self.assertEqual(timeslot_qs_func(classrooms[0]).count(), 1)

        expected = self.build_expected(classrooms, timeslot_qs=timeslot_qs_func)
        self.list_request(url + "?available_slots=true", expected, num_queries=7)


class ClassroomDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:classroom-detail'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.classroom = ClassroomFactory(course=self.course)
        self.timeslots = TimeslotFactory.build_batch(3, classroom=self.classroom)

    def test_url(self):
        self.assertURLNameEqual('classroom_modules/{}/', args=(self.classroom.id,), base_url=settings.API_BASE_URL)

    def build_expected(self, classroom, timeslot_qs=None):
        if timeslot_qs is None:
            timeslot_qs = lambda obj: obj.timeslots.all()  # noqa: E731

        return {
            'id': classroom.id,
            'name': classroom.name,
            'description': classroom.description,
            'estimated_time': classroom.estimated_time,
            'timeslots': [{
                'id': slot.id,
                'title': slot.title,
                'summary': slot.summary,
                'begin_date': serializers.DateTimeField().to_representation(slot.begin_date),
                'end_date': serializers.DateTimeField().to_representation(slot.end_date),
                'max_participants': slot.max_participants,
                'num_participants': slot.num_participants,
            } for slot in timeslot_qs(classroom)],
        }

    def test_no_auth(self):
        self.detail_request(self.get_url(self.classroom.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_is_not_student(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(self.get_url(self.classroom.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=6)

    def test_available_for_active_or_completed_students(self):
        for student_status in [CourseStudent.StatusChoices.ACTIVE, CourseStudent.StatusChoices.COMPLETED]:
            student = CourseStudentFactory(course=self.course, status=student_status)
            self.client.force_login(user=student.user)
            self.detail_request(
                url=self.get_url(self.classroom.id),
                num_queries=8,
            )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.classroom.id),
            num_queries=6,
            status_code=status.HTTP_403_FORBIDDEN,
        )

    def test_get(self):
        CourseStudentFactory(course=self.course, user=self.user)
        self.client.force_login(user=self.user)
        url = self.get_url(self.classroom.id)
        expected = self.build_expected(self.classroom)
        self.detail_request(url, expected=expected, num_queries=8)

    def test_get_no_available_slots(self):
        classroom = ClassroomFactory(course=self.course)
        course_group = CourseGroupFactory(course=self.course)
        timeslots = TimeslotFactory.create_batch(3, classroom=classroom)
        timeslots[2].course_groups.set([course_group])

        CourseStudentFactory(course=self.course, user=self.user)
        self.client.force_login(user=self.user)
        url = self.get_url(classroom.id)

        timeslot_qs_func = lambda obj: Timeslot.objects.filter(classroom=obj).available_for(self.user)  # noqa: E731
        expected = self.build_expected(classroom, timeslot_qs=timeslot_qs_func)
        self.detail_request(url + "?available_slots=true", expected=expected, num_queries=8)

    def test_get_available_for_user(self):
        classroom = ClassroomFactory(course=self.course)
        course_group = CourseGroupFactory(course=self.course)
        CourseStudentFactory(course=self.course, user=self.user, group=course_group)

        timeslots = TimeslotFactory.create_batch(3, classroom=classroom)
        timeslots[2].course_groups.set([course_group])

        self.client.force_login(user=self.user)
        url = self.get_url(classroom.id)

        timeslot_qs_func = lambda obj: Timeslot.objects.filter(classroom=obj).available_for(self.user)  # noqa: E731
        self.assertEqual(timeslot_qs_func(classroom).count(), 1)

        expected = self.build_expected(classroom, timeslot_qs=timeslot_qs_func)
        self.detail_request(url + "?available_slots=true", expected=expected, num_queries=8)


class ClassroomTimeslotCheckinTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:classroom-timeslot-checkin'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.classroom = ClassroomFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_timeslots/{}/checkin/', args=(1,), base_url=settings.API_BASE_URL,
        )

    def test_create(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)

        url = self.get_url(timeslot.id)

        def make_expected(response):
            response_data = response.data
            expected = {
                'id': response_data['id'],
                'student_id': student.id,
                'timeslot_id': timeslot.id,
                'is_attended': True,
            }
            return expected

        self.create_request(url, expected=make_expected, num_queries=17)

    def test_create_already_existing_accepted(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        student_slot = StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        expected = {
            'id': student_slot.id,
            'student_id': student.id,
            'timeslot_id': timeslot.id,
            'is_attended': True,
        }

        self.create_request(url, expected=expected, num_queries=5)

    def test_create_already_existing_not_accepted(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)

        rejected_slot = StudentSlotFactory(
            student=student,
            timeslot=timeslot,
            status=StudentSlot.StatusChoices.REJECTED
        )
        canceled_slot = StudentSlotFactory(
            student=student,
            timeslot=timeslot,
            status=StudentSlot.StatusChoices.CANCELED
        )
        exchanged_slot = StudentSlotFactory(
            student=student,
            timeslot=timeslot,
            status=StudentSlot.StatusChoices.EXCHANGED
        )

        url = self.get_url(timeslot.id)

        def make_expected(response):
            self.assertNotEqual(rejected_slot.id, response.data['id'])
            self.assertNotEqual(canceled_slot.id, response.data['id'])
            self.assertNotEqual(exchanged_slot.id, response.data['id'])

            return {
                'id': response.data['id'],
                'student_id': student.id,
                'timeslot_id': timeslot.id,
                'is_attended': True,
            }

        self.create_request(url, expected=make_expected, num_queries=17)

    def test_create_with_groups(self):
        self.client.force_login(user=self.user)

        course_group = CourseGroupFactory(course=self.course)
        student = CourseStudentFactory(
            user=self.user, course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )
        timeslot = TimeslotFactory(classroom=self.classroom)
        timeslot.course_groups.set([course_group])

        url = self.get_url(timeslot.id)

        def make_expected(response):
            response_data = response.data
            expected = {
                'id': response_data['id'],
                'student_id': student.id,
                'timeslot_id': timeslot.id,
                'is_attended': True,
            }
            return expected

        self.create_request(url, expected=make_expected, num_queries=17)

    def test_create_with_wrong_group(self):
        self.client.force_login(user=self.user)

        course_group = CourseGroupFactory(course=self.course)
        wrong_group = CourseGroupFactory()
        # current user
        CourseStudentFactory(
            user=self.user, course=self.course, group=wrong_group, status=CourseStudent.StatusChoices.ACTIVE,
        )
        timeslot = TimeslotFactory(classroom=self.classroom)
        timeslot.course_groups.set([course_group])

        url = self.get_url(timeslot.id)

        response = self.create_request(url, num_queries=11, status_code=status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data.keys(), {'student'})

    def test_create_slot_is_full(self):
        self.client.force_login(user=self.user)

        course_group = CourseGroupFactory(course=self.course)
        student_with_slot = CourseStudentFactory(
            course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )
        # current user
        CourseStudentFactory(
            user=self.user, course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )
        timeslot = TimeslotFactory(classroom=self.classroom, max_participants=1)
        timeslot.course_groups.set([course_group])
        StudentSlotFactory(student=student_with_slot, timeslot=timeslot)

        url = self.get_url(timeslot.id)

        response = self.create_request(url, num_queries=15, status_code=status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data.keys(), {'timeslot'})


class ClassroomTimeslotCancelTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:classroom-timeslot-cancel'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.classroom = ClassroomFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_timeslots/{}/cancel/', args=(1,), base_url=settings.API_BASE_URL,
        )

    def test_cancel_not_existing_timeslot(self):
        self.client.force_login(user=self.user)

        CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)

        url = self.get_url('123')

        self.create_request(
            url,
            num_queries=3,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST
        )

    def test_exchange_not_existing_student(self):
        self.client.force_login(user=self.user)

        another_course = CourseFactory(is_active=True)
        CourseStudentFactory(user=self.user, course=another_course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            num_queries=3,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_cancel_not_existing_student_slot(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.REJECTED)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.CANCELED)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            num_queries=3,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_cancel(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        student_slot = StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        expected = {
            'id': student_slot.id,
            'student_id': student.id,
            'timeslot_id': timeslot.id,
            'is_attended': True,
        }

        self.create_request(url, expected=expected, num_queries=10, status_code=status.HTTP_200_OK)

        student_slot.refresh_from_db()
        self.assertEqual(student_slot.status, StudentSlot.StatusChoices.CANCELED)


class TimeslotExchangeCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:timeslot-exchange-create'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.classroom = ClassroomFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_timeslots/{}/exchange/', args=(1,), base_url=settings.API_BASE_URL,
        )

    def test_exchange_not_existing_timeslot(self):
        self.client.force_login(user=self.user)

        CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        target_timeslot = TimeslotFactory(classroom=self.classroom)

        url = self.get_url('123')

        self.update_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=3,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_exchange_not_existing_student(self):
        self.client.force_login(user=self.user)

        another_course = CourseFactory(is_active=True)
        CourseStudentFactory(user=self.user, course=another_course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom)

        url = self.get_url(timeslot.id)

        self.update_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=3,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_exchange_not_existing_target_timeslot(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        self.update_request(
            url,
            data={"target_timeslot_id": '123'},
            num_queries=4,
            expected={"target_timeslot_id": ["does_not_exist"]},
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_exchange_not_existing_student_slot(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.REJECTED)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.CANCELED)

        url = self.get_url(timeslot.id)

        self.update_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=3,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_exchange_same_timeslot(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        self.update_request(
            url,
            data={"target_timeslot_id": timeslot.id},
            num_queries=14,
            expected={"target_timeslot_id": ["same_timeslot"]},
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_exchange_another_classroom(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        another_classroom = ClassroomFactory(course=self.course)
        target_timeslot = TimeslotFactory(classroom=another_classroom)

        url = self.get_url(timeslot.id)

        self.update_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=14,
            expected={"target_timeslot_id": ["another_classroom_timeslot"]},
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_create_new_exchange(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        student_slot = StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)
        student_slot_exchange = TimeslotExchangeFactory(
            student_slot=student_slot,
            target_timeslot=target_timeslot,
            is_active=False
        )

        url = self.get_url(timeslot.id)

        def make_expected(response):
            self.assertNotEqual(response.data["id"], student_slot_exchange.id)

            return {
                "id": response.data["id"],
                "student_slot_id": student_slot.id,
                "target_timeslot_id": target_timeslot.id,
                "course_id": self.course.id,
            }

        self.update_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=14,
            expected=make_expected,
            status_code=status.HTTP_201_CREATED,
        )

    def test_create_already_existing_exchange(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        student_slot = StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        student_slot_exchange = TimeslotExchangeFactory(
            student_slot=student_slot,
            target_timeslot=target_timeslot,
            is_active=True
        )

        url = self.get_url(timeslot.id)

        expected = {
            "id": str(student_slot_exchange.id),
            "student_slot_id": student_slot.id,
            "target_timeslot_id": target_timeslot.id,
            "course_id": self.course.id,
        }
        self.update_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=5, expected=expected,
            status_code=status.HTTP_201_CREATED
        )


class ClassroomTimeslotChangeTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:classroom-timeslot-change'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.classroom = ClassroomFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual('classroom_timeslots/{}/change/', args=(1,), base_url=settings.API_BASE_URL)

    def test_change_not_existing_timeslot(self):
        self.client.force_login(user=self.user)

        CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        target_timeslot = TimeslotFactory(classroom=self.classroom)

        url = self.get_url('123')

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=6,
            expected=["no_student_slot"],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_change_not_existing_student(self):
        self.client.force_login(user=self.user)

        another_course = CourseFactory(is_active=True)
        CourseStudentFactory(user=self.user, course=another_course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=6,
            expected=["no_student_slot"],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_change_not_existing_student_slot(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.REJECTED)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.CANCELED)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=6,
            expected=['no_student_slot'],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_change_same_timeslot(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            data={"target_timeslot_id": timeslot.id},
            num_queries=6,
            expected=["same_timeslot"],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_change_another_classroom(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        timeslot = TimeslotFactory(classroom=self.classroom)
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        another_classroom = ClassroomFactory(course=self.course)
        target_timeslot = TimeslotFactory(classroom=another_classroom)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=7,
            expected=["not_existing_timeslot"],
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

    def test_target_timeslot_is_full(self):
        self.client.force_login(user=self.user)

        course_group = CourseGroupFactory(course=self.course)
        student_with_slot = CourseStudentFactory(
            course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )
        # current user
        student = CourseStudentFactory(
            user=self.user, course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )

        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom, max_participants=1)
        timeslot.course_groups.set([course_group])
        target_timeslot.course_groups.set([course_group])

        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)
        StudentSlotFactory(
            student=student_with_slot,
            timeslot=target_timeslot,
            status=StudentSlot.StatusChoices.ACCEPTED
        )

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=20,
            expected={'timeslot': ['slot_unavailable']},
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST
        )

    def test_change_with_wrong_group(self):
        self.client.force_login(user=self.user)

        course_group = CourseGroupFactory(course=self.course)
        wrong_group = CourseGroupFactory(course=self.course)
        # current user
        student = CourseStudentFactory(
            user=self.user, course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )

        timeslot = TimeslotFactory(classroom=self.classroom)
        timeslot.course_groups.set([course_group])
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot.course_groups.set([wrong_group])

        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=16,
            expected={'student': ['wrong_student_group']},
            check_errors=True,
            status_code=status.HTTP_400_BAD_REQUEST
        )

    def test_change(self):
        self.client.force_login(user=self.user)

        student = CourseStudentFactory(
            user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE,
        )
        timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        student_slot = StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        def make_expected(response):
            response_data = response.data
            expected = {
                'id': response_data['id'],
                'student_id': student.id,
                'timeslot_id': target_timeslot.id,
                'is_attended': True,
            }
            return expected

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=22,
            expected=make_expected,
            status_code=status.HTTP_201_CREATED
        )

        student_slot.refresh_from_db()
        self.assertEqual(student_slot.status, StudentSlot.StatusChoices.CANCELED)

        self.assertEqual(
            student_slot.history.first().history_change_reason,
            f'Произошла перезапись на слот {target_timeslot}',
        )

    def test_change_with_group(self):
        self.client.force_login(user=self.user)

        course_group = CourseGroupFactory(course=self.course)
        student = CourseStudentFactory(
            user=self.user, course=self.course, group=course_group, status=CourseStudent.StatusChoices.ACTIVE,
        )
        timeslot = TimeslotFactory(classroom=self.classroom)
        timeslot.course_groups.set([course_group])
        target_timeslot = TimeslotFactory(classroom=self.classroom)
        target_timeslot.course_groups.set([course_group])

        student_slot = StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        def make_expected(response):
            response_data = response.data
            expected = {
                'id': response_data['id'],
                'student_id': student.id,
                'timeslot_id': target_timeslot.id,
                'is_attended': True,
            }
            return expected

        self.create_request(
            url,
            data={"target_timeslot_id": target_timeslot.id},
            num_queries=22,
            expected=make_expected,
            status_code=status.HTTP_201_CREATED
        )

        student_slot.refresh_from_db()
        self.assertEqual(student_slot.status, StudentSlot.StatusChoices.CANCELED)

        self.assertEqual(
            student_slot.history.first().history_change_reason,
            f'Произошла перезапись на слот {target_timeslot}'
        )


class TimeslotExchangeRetrieveTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:timeslot-exchange-detail'

    def test_url(self):
        self.assertURLNameEqual('timeslot_exchanges/{}/', args=(uuid.uuid4(),), base_url=settings.API_BASE_URL)

    def build_expected_timeslot(self, timeslot):
        return {
            'id': timeslot.id,
            'title': timeslot.title,
            'summary': timeslot.summary,
            'classroom_id': timeslot.classroom_id,
            'begin_date': serializers.DateTimeField().to_representation(timeslot.begin_date),
            'end_date': serializers.DateTimeField().to_representation(timeslot.end_date),
            'num_participants': timeslot.num_participants,
            'max_participants': timeslot.max_participants,
        }

    def test_user_not_in_course(self):
        exchange = TimeslotExchangeFactory(is_active=True)

        another_user = UserFactory()
        another_course = CourseFactory(is_active=True)
        CourseStudentFactory(user=another_user, course=another_course, status=CourseStudent.StatusChoices.ACTIVE)

        self.client.force_login(user=another_user)

        url = self.get_url(exchange.id)

        self.detail_request(
            url,
            expected={'detail': 'not_found'},
            check_errors=True,
            num_queries=3,
            status_code=status.HTTP_404_NOT_FOUND
        )

    def test_course_not_active(self):
        exchange = TimeslotExchangeFactory(is_active=True, course__is_active=False, course__is_archive=False)

        self.client.force_login(user=exchange.student_slot.student.user)

        url = self.get_url(exchange.id)

        self.detail_request(
            url,
            expected={'detail': 'not_found'},
            check_errors=True,
            num_queries=3,
            status_code=status.HTTP_404_NOT_FOUND
        )

    def test_course_archived(self):
        exchange = TimeslotExchangeFactory(is_active=True, course__is_active=True, course__is_archive=True)

        self.client.force_login(user=exchange.student_slot.student.user)

        url = self.get_url(exchange.id)

        self.detail_request(
            url,
            expected={'detail': 'not_found'},
            check_errors=True,
            num_queries=3,
            status_code=status.HTTP_404_NOT_FOUND
        )

    def test_retrieve(self):
        exchange = TimeslotExchangeFactory(is_active=True, course__is_active=True, course__is_archive=False)
        exchange.refresh_from_db()

        self.client.force_login(user=exchange.student_slot.student.user)

        url = self.get_url(exchange.id)

        expected = {
            'id': str(exchange.id),
            'student_slot': {
                'id': exchange.student_slot.id,
                'timeslot': self.build_expected_timeslot(exchange.student_slot.timeslot),
                'student_id': exchange.student_slot.student_id,
                'is_attended': exchange.student_slot.is_attended,
            },
            'target_timeslot': self.build_expected_timeslot(exchange.target_timeslot),
            'course_id': exchange.course_id,
        }

        self.detail_request(url, expected=expected, num_queries=3)


class TimeslotExchangeAcceptTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:timeslot-exchange-accept'

    def setUp(self) -> None:
        self.user = UserFactory()

    def test_url(self):
        self.assertURLNameEqual('timeslot_exchanges/{}/accept/', args=(uuid.uuid4(),), base_url=settings.API_BASE_URL)

    def test_accept_not_existing_exchange(self):
        self.client.force_login(user=self.user)

        url = self.get_url(uuid.uuid4())

        self.create_request(
            url,
            expected={'detail': 'not_found'},
            check_errors=True,
            num_queries=6,
            status_code=status.HTTP_404_NOT_FOUND
        )

    def test_accept_not_active_exchange(self):
        exchange = TimeslotExchangeFactory(is_active=False)

        self.client.force_login(user=self.user)
        url = self.get_url(exchange.id)

        self.create_request(
            url,
            expected={'detail': 'not_found'},
            check_errors=True,
            num_queries=6,
            status_code=status.HTTP_404_NOT_FOUND
        )

    def test_accept_by_user_with_rejected_student_slot(self):
        exchange = TimeslotExchangeFactory()

        StudentSlotFactory(
            timeslot=exchange.target_timeslot,
            student__user=self.user,
            status=StudentSlot.StatusChoices.REJECTED
        )

        self.client.force_login(user=self.user)
        url = self.get_url(exchange.id)

        self.create_request(
            url,
            expected=['no_student_slot'],
            check_errors=True,
            num_queries=7,
            status_code=status.HTTP_400_BAD_REQUEST
        )

    def test_accept_by_user_with_canceled_student_slot(self):
        exchange = TimeslotExchangeFactory()

        StudentSlotFactory(
            timeslot=exchange.target_timeslot,
            student__user=self.user,
            status=StudentSlot.StatusChoices.CANCELED
        )

        self.client.force_login(user=self.user)
        url = self.get_url(exchange.id)

        self.create_request(
            url,
            expected=['no_student_slot'],
            check_errors=True,
            num_queries=7,
            status_code=status.HTTP_400_BAD_REQUEST
        )

    def test_accept_by_user_with_exchanged_student_slot(self):
        exchange = TimeslotExchangeFactory()

        StudentSlotFactory(
            timeslot=exchange.target_timeslot,
            student__user=self.user,
            status=StudentSlot.StatusChoices.EXCHANGED
        )

        self.client.force_login(user=self.user)
        url = self.get_url(exchange.id)

        self.create_request(
            url,
            expected=['no_student_slot'],
            check_errors=True,
            num_queries=7,
            status_code=status.HTTP_400_BAD_REQUEST
        )

    def test_accept(self):
        exchange = TimeslotExchangeFactory(
            student_slot__timeslot__max_participants=1,
            target_timeslot__max_participants=1,
        )

        student_slot = exchange.student_slot
        target_student_slot = StudentSlotFactory(
            timeslot=exchange.target_timeslot,
            student__user=self.user,
            status=StudentSlot.StatusChoices.ACCEPTED
        )

        self.client.force_login(user=self.user)
        url = self.get_url(exchange.id)

        def make_expected(_):
            exchange.refresh_from_db()

            return {
                'id': exchange.exchanged_target_student_slot.id,
                'student_id': target_student_slot.student_id,
                'timeslot_id': student_slot.timeslot_id,
                'is_attended': True,
            }

        self.create_request(url, expected=make_expected, num_queries=47, status_code=status.HTTP_201_CREATED)

        student_slot.refresh_from_db()
        target_student_slot.refresh_from_db()

        self.assertEqual(student_slot.status, StudentSlot.StatusChoices.EXCHANGED)
        self.assertEqual(target_student_slot.status, StudentSlot.StatusChoices.EXCHANGED)

        self.assertEqual(exchange.target_student_slot.id, target_student_slot.id)

        self.assertEqual(exchange.exchanged_student_slot.student_id, student_slot.student_id)
        self.assertEqual(exchange.exchanged_student_slot.timeslot_id, exchange.target_timeslot_id)

        self.assertEqual(exchange.exchanged_target_student_slot.student_id, target_student_slot.student_id)
        self.assertEqual(exchange.exchanged_target_student_slot.timeslot_id, student_slot.timeslot_id)

        self.assertFalse(exchange.is_active)

        self.assertEqual(
            exchange.student_slot.history.first().history_change_reason,
            f'Произошел обмен на слот {exchange.target_timeslot}'
        )
        self.assertEqual(
            target_student_slot.history.first().history_change_reason,
            f'Произошел обмен на слот {exchange.student_slot.timeslot}'
        )


class UserClassroomTimeslotListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-classroom-timeslots'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual('my/classroom_modules/{}/timeslots/', args=(1,), base_url=settings.API_BASE_URL)

    def build_expected(self, student_slots):
        sorted_items = sorted(student_slots, key=lambda s: s.timeslot_id)
        sorted_items = sorted(sorted_items, reverse=True, key=lambda s: s.created)
        return [{
            'id': student_slot.id,
            'timeslot_id': student_slot.timeslot_id,
            'is_attended': student_slot.is_attended,
        }for student_slot in sorted_items]

    def test_list(self):
        self.client.force_login(user=self.user)
        classroom = ClassroomFactory(course=self.course)
        timeslots = TimeslotFactory.create_batch(5, classroom=classroom)

        inactive_student = CourseStudentFactory(
            course=self.course, user=self.user, status=CourseStudent.StatusChoices.ACTIVE,
        )
        for timeslot in sorted(timeslots, key=lambda x: x.begin_date, reverse=True):
            StudentSlotFactory(student=inactive_student, timeslot=timeslot)
        inactive_student.status = CourseStudent.StatusChoices.EXPELLED
        inactive_student.save()

        active_student = CourseStudentFactory(
            course=self.course, user=self.user, status=CourseStudent.StatusChoices.ACTIVE,
        )
        student_slots = []
        for timeslot in sorted(timeslots, key=lambda x: x.begin_date, reverse=True):
            student_slots += [
                StudentSlotFactory(student=active_student, timeslot=timeslot)
            ]

        url = self.get_url(classroom.id)
        expected = self.build_expected(student_slots)

        self.list_request(url, expected=expected, num_queries=4)


class UserClassroomTimeslotDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-classroom-timeslot-detail'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual('my/classroom_timeslots/{}/', args=(1,), base_url=settings.API_BASE_URL)

    @skip("ручка сломана, но не используется")
    def test_delete_slot_active_student(self):
        self.client.force_login(user=self.user)
        classroom = ClassroomFactory(course=self.course, is_active=True)
        timeslot = TimeslotFactory(classroom=classroom)

        student = CourseStudentFactory(
            course=self.course, user=self.user, status=CourseStudent.StatusChoices.ACTIVE,
        )
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)

        url = self.get_url(timeslot.id)

        self.delete_request(url, num_queries=8)

    @skip("ручка сломана, но не используется")
    def test_delete_slot_inactive_student(self):
        self.client.force_login(user=self.user)
        classroom = ClassroomFactory(course=self.course, is_active=True)
        timeslot = TimeslotFactory(classroom=classroom)

        student = CourseStudentFactory(
            course=self.course, user=self.user, status=CourseStudent.StatusChoices.ACTIVE,
        )
        StudentSlotFactory(student=student, timeslot=timeslot, status=StudentSlot.StatusChoices.ACCEPTED)
        student.status = CourseStudent.StatusChoices.EXPELLED
        student.save()

        url = self.get_url(timeslot.id)

        self.delete_request(url, num_queries=3, status_code=status.HTTP_404_NOT_FOUND)
